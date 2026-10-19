-- P3-F10：归一化历史动作版本的姿态引擎标识。
-- 患者端本地推理引擎（onnxruntime-web 1.30.0 加载 MoveNet Lightning，
-- 模型 sha256 前 12 位 1ad4f8d6c2f7）的真实标识为：
--   movenet-lightning-web@1ad4f8d6c2f7-ortweb1.30.0
-- P3 前的联调数据写入了占位值（movenet-v1 / movenet-lightning-web@v1 / e@v1），
-- 会导致患者端快照与端侧引擎比对失败（422 拒绝开始），这里统一回填。
-- 仅修正已知占位值，不覆盖将来可能接入的其他引擎标识。

UPDATE action_version
SET pose_engine_id = 'movenet-lightning-web@1ad4f8d6c2f7-ortweb1.30.0'
WHERE pose_engine_id IN ('movenet-v1', 'movenet-lightning-web@v1', 'e@v1');
