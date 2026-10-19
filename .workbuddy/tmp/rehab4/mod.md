（1）模块职责。姿态评估与训练评分模块由用户端前端的姿态评估引擎（frontend-app/src/pose-engine）与后端训练记录接口共同实现，负责四件事：把治疗师配置的评估规则与算法参数下发到端侧；在端侧完成关键点提取、关节角度计算、动作周期切分与实时纠错提示；训练结束后对整段帧序列做确定性复算，生成六维评分与纠错摘要；把汇总结果提交服务端落库，写入训练记录表（training_record）与训练纠错日志表（training_error_log），并据此更新每日训练任务状态。

（2）处理流程。模块分为实时路径与结束路径两条链路。实时路径逐帧执行、因果计算，用于即时反馈：请求动作快照 → 校验姿态引擎版本 → 加载本地 MoveNet 模型并校验文件摘要 → 摄像头采集视频帧 → 推理得到 17 个关键点 → 入场检查与质量门控 → 在线平滑与关键点修复 → 计算关节角度与左右对称差 → 阶段状态机计数 → 纠错提示入队 → 渲染骨架、计数与提示。结束路径在训练结束时执行、非因果、结果可复现，用于最终成绩：重放本地缓存的原始帧 → 序列级修复 → 特征重建 → 状态机重放切分周期 → 动作签名校验 → 纠错区间聚合 → 六维加权评分 → 生成训练汇总并提交服务端。整体流程如图9所示。

@@FIG09@@

图9　姿态评估与训练评分模块处理流程

（3）实时推理在浏览器端本地完成。模型清单（模型文件名、字节数、SHA-256、输入输出形状、关键点顺序、运行时版本）随静态资源一起分发，加载后先对模型文件计算 SHA-256 并与清单比对，校验不通过立即抛出明确错误并由页面引导重试，而不是白屏；推理会话按 WebGL 到单线程 WASM 的顺序尝试，并对会话创建加超时竞速，避免个别环境下推理 worker 静默不就绪导致整页永久卡死。关键代码见下面的代码1。

代码1　本地模型加载、完整性校验与后端降级（movenet.ts 节选）

// Compute SHA-256 of the model file and compare it with the manifest

const modelBuffer = await (await fetch(modelUrl)).arrayBuffer()

const actualSha = toHex(await crypto.subtle.digest('SHA-256', modelBuffer))

if (actualSha !== manifest.sha256) throw new Error('model integrity check failed')

// Create the inference session with a timeout race; webgl first, wasm as fallback

const input = new ort.Tensor('int32', new Int32Array(inputSize * inputSize * 3), [1, inputSize, inputSize, 3])

const session = await Promise.race([

  ort.InferenceSession.create(modelBuffer.slice(0), { executionProviders: [provider] }),

  new Promise((_, rej) => setTimeout(() => rej(new Error('backend not ready')), timeoutMs))

])

await session.run({ [session.inputNames[0]]: input })   // probe: created session does not mean executable

（4）关节角度与左右对称配对。关键点的坐标全部换算为视频帧归一化坐标后，用三点夹角公式计算关节角度：顶点取中间的关节，返回值 0 到 180 度，完全伸直为 180 度；同时把解剖学上的左右关节映射为配对关系，供对称维度与对称配对规则使用，见代码2。

代码2　三点夹角与左右配对（geometry.ts 节选）

// Angle at vertex b, formed by a-b-c, in degrees (0~180)

export function angleAt(a: Point, b: Point, c: Point): number {

  const v1x = a.x - b.x, v1y = a.y - b.y

  const v2x = c.x - b.x, v2y = c.y - b.y

  const dot = v1x * v2x + v1y * v2y

  const n = Math.hypot(v1x, v1y) * Math.hypot(v2x, v2y)

  if (n < 1e-9) return 0

  return (Math.acos(clamp(dot / n, -1, 1)) * 180) / Math.PI

}

// Joint name -> keypoint triple (vertex in the middle) and left/right pairs

const JOINT_TRIPLETS = {

  left_elbow: [KP.L_SHOULDER, KP.L_ELBOW, KP.L_WRIST],

  right_elbow: [KP.R_SHOULDER, KP.R_ELBOW, KP.R_WRIST],

  left_knee: [KP.L_HIP, KP.L_KNEE, KP.L_ANKLE],

  right_knee: [KP.R_HIP, KP.R_KNEE, KP.R_ANKLE]

}

（5）动作周期切分与计数。关节角度序列送入双阈值滞回状态机，按 READY、RAISING、HOLD、LOWERING 再回到 READY 的状态迁移切出一个个动作周期：用容差区间加 8 度滞回带避免临界抖动造成的反复切换，要求连续若干帧确认才真正切换状态，低置信度帧不参与切换；一个完整周期还必须满足最短时长与最小幅度门槛才计入达标次数，过短的周期视为甩动直接丢弃，见代码3。

代码3　双阈值滞回状态机（state-machine.ts 节选）

// Phase machine: READY -> RAISING -> HOLD -> LOWERING -> READY, one full cycle

private tick(input: MachineTickInput): void {

  // The hysteresis band (8 degrees) prevents flickering on the boundary

  const enter = this.polarity > 0 ? this.params.peakLow + this.hysteresis : this.params.peakHigh - this.hysteresis

  // A phase switch is confirmed only after N consecutive switchable frames

  if (this.pendingFrames >= this.conf.stateHoldFrames) this.phase = next

  // A cycle shorter than minCycleSec is treated as a swing and dropped

  if (cycle.durationSec < this.conf.minCycleSec || cycle.rom < this.params.minRom) cycle.ignored = true

}

（6）六维加权评分与 DTW 相似度。训练结束后对每个有效周期分别计算六个维度得分：幅度（与目标幅度或签名峰值区间比较）、保持（保持帧数换算为秒）、节奏（与期望周期时长的吻合度占 60%，周期间节拍一致性占 40%）、稳定（周期内角度抖动的残差 RMS 指数衰减）、对称（左右角度差均值）、相似度（与模板特征序列的 DTW 距离）。总分按六维权重加权平均，权重不归一时代码自动归一；模板缺失时相似度自动降权为 0，不阻塞评分；有效帧占比不足或关键点修复比例超限时姿态质量判为 INSUFFICIENT，记录但不计分，避免用不可信数据给出成绩，见代码4。

代码4　DTW 距离与六维加权聚合（scoring.ts 节选）

// Classic DTW with a window constraint, normalized by the path length

export function dtwDistance(a: number[], b: number[], windowRatio = 0.2): number {

  const n = a.length, m = b.length

  const w = Math.max(1, Math.floor(Math.max(n, m) * windowRatio))

  const cost = Array.from({ length: n + 1 }, () => new Float64Array(m + 1).fill(Infinity))

  cost[0][0] = 0

  for (let i = 1; i <= n; i++) {

    for (let j = Math.max(1, i - w); j <= Math.min(m, i + w); j++) {

      cost[i][j] = Math.abs(a[i - 1] - b[j - 1])

        + Math.min(cost[i - 1][j], cost[i][j - 1], cost[i - 1][j - 1])

    }

  }

  return cost[n][m] / (n + m)

}

// Six weighted dimensions -> total score (weights are normalized automatically)

const effectiveWeightSum = weights.amplitude + weights.hold + weights.rhythm +

  weights.stability + weights.symmetry + weights.similarity

total = (detail.amplitude * weights.amplitude + detail.hold * weights.hold +

  detail.rhythm * weights.rhythm + detail.stability * weights.stability +

  detail.symmetry * weights.symmetry + detail.similarity * weights.similarity) / effectiveWeightSum

（7）结果提交与幂等落库。端侧只上传汇总结果（总次数、达标次数、综合得分、六维明细、质量指标与纠错摘要），原始视频与逐帧关键点不出浏览器，既保护隐私也避免大流量上传。提交请求携带客户端唯一标识 clientRequestId，服务端以（daily_task_id, client_request_id）的唯一约束保证同一任务的重复提交只落一条记录，天然实现幂等；请求体同时携带动作版本与姿态引擎标识，服务端校验二者与计划项、动作版本一致，保证评分可比，见代码5。

代码5　训练汇总提交请求（TrainingRecordSubmitRequest.java 节选）

@Data

@Schema(description = "Training summary submit request")

public class TrainingRecordSubmitRequest implements Serializable {

    // Idempotency key: the same value returns the first result

    @NotBlank(message = "clientRequestId is required")

    private String clientRequestId;

    @NotNull

    private Long dailyTaskId;

    @NotNull

    private Long actionVersionId;

    // Total / correct reps and the 0~100 total score

    @Min(0)

    private Integer totalReps;

    @Min(0)

    private Integer correctReps;

    @DecimalMin("0")

    @DecimalMax("100")

    private BigDecimal totalScore;

    // Six-dimension detail, e.g. amplitude / hold / rhythm

    private Map<String, BigDecimal> scoreDetail;

    @NotNull

    private PoseQuality poseQuality;

    @NotBlank

    private String poseEngineId;

}

（8）运行效果与数据落库。用户端训练页在进入任务时先向服务端请求动作快照（含动作规则、算法配置与标准模板），并校验姿态引擎版本是否与动作版本一致；随后打开摄像头，页面实时叠加骨架、动作阶段、已完成次数与纠错提示，训练过程中不产生任何网络请求。训练结束后引擎给出本次训练汇总，页面展示总分、六维得分与主要纠错项，并将结果提交到服务端落库；治疗师可在管理端查看该患者的训练记录、分项得分与纠错明细，并按计划周期生成评估报告。
