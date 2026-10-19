# -*- coding: utf-8 -*-
"""向论文追加 结 论 / 致 谢 / 参考文献 三部分。
参考文献采用开题报告中的 17 条，五号（10.5pt）+ 悬挂缩进。"""
import os
from docx import Document
from docx.shared import Pt, Cm
from docx.enum.text import WD_ALIGN_PARAGRAPH, WD_LINE_SPACING
from docx.oxml.ns import qn
from docx.oxml import OxmlElement

DOC = r"F:\基于springboot框架MoveNet模型人体姿态评估康复训练辅助平台\论文初稿加开题答辩\毕业论文初稿-基于SpringBoot与MoveNet的康复训练辅助平台.docx"

CN, EN = "宋体", "Times New Roman"


def run_font(run, cn=CN, en=EN, size=None, bold=None):
    if size is not None:
        run.font.size = Pt(size)
    if bold is not None:
        run.font.bold = bold
    run.font.name = en
    rpr = run._r.get_or_add_rPr()
    rf = rpr.find(qn('w:rFonts'))
    if rf is None:
        rf = OxmlElement('w:rFonts')
        rpr.insert(0, rf)
    for a in list(rf.attrib):
        del rf.attrib[a]
    rf.set(qn('w:ascii'), en)
    rf.set(qn('w:hAnsi'), en)
    rf.set(qn('w:eastAsia'), cn)
    rf.set(qn('w:cs'), en)


def first_line_indent_chars(p, chars=200):
    pPr = p._p.get_or_add_pPr()
    ind = pPr.find(qn('w:ind'))
    if ind is None:
        ind = OxmlElement('w:ind')
        pPr.append(ind)
    ind.set(qn('w:firstLineChars'), str(chars))
    ind.set(qn('w:firstLine'), '480')


def body(doc, text, size=12):
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY
    p.paragraph_format.line_spacing = Pt(22)
    p.paragraph_format.line_spacing_rule = WD_LINE_SPACING.EXACTLY
    first_line_indent_chars(p)
    r = p.add_run(text)
    run_font(r, CN, EN, size)
    return p


def ref_item(doc, text):
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.LEFT
    p.paragraph_format.line_spacing = 1.0
    p.paragraph_format.space_before = Pt(0)
    p.paragraph_format.space_after = Pt(0)
    p.paragraph_format.left_indent = Cm(0.85)
    p.paragraph_format.first_line_indent = Cm(-0.85)
    r = p.add_run(text)
    run_font(r, CN, EN, 10.5)
    return p


doc = Document(DOC)

# ======================= 结  论 =======================
doc.add_page_break()
doc.add_paragraph('结  论', style='Heading 1')

body(doc, "本文围绕居家康复训练中“指导不足、动作不规范、训练数据缺失”的实际问题，设计并实现了一个基于 Spring Boot 框架与 MoveNet 姿态评估模型的康复训练辅助平台。论文按照软件工程的流程展开，依次完成了需求分析、系统设计、系统实现与系统测试。平台面向康复患者、康复治疗师与系统管理员三类角色，划分为账户与权限、动作与算法、康复计划、姿态评估与训练、数据与报告、公共内容与文件、咨询与运维七个业务域，将“计划—训练—评估—报告”的康复流程数字化，实现了从患者与治疗师绑定、治疗师制定专属康复计划、患者按计划完成训练、系统实时姿态评估与纠错，到保存训练记录、生成并审核阶段性评估报告的完整闭环。")

body(doc, "在技术实现上，本文的主要工作与特点体现在以下几个方面。其一，采用“三端 + 七大业务域”的功能划分，并与数据库概念模型、E-R 图及业务流程逐一对应，使功能、数据与流程之间形成清晰的追溯关系。其二，设计并实现了一条可在浏览器端运行的确定性姿态评估后处理链，在 MoveNet 推理之后依次完成三级置信度门控、中值／EMA 时序平滑与遮挡几何修复、骨架归一化、运动特征提取、动作签名校验与阶段状态机计数，并由幅度、保持、节奏、稳定、对称与模板相似度六个维度加权评分，在保证实时性的同时提升了评估的可解释性。其三，坚持隐私最小化的设计原则，原始视频与逐帧关键点数据始终保留在浏览器本地，仅上传汇总指标与质量信息，从架构层面降低了健康数据泄露的风险。其四，对康复计划与动作评估规则实行版本化的不可变管理，并将算法参数随动作版本一并快照，使历史训练记录与评估标准可追溯、评分结果可复现。其五，明确 AI 的使用边界，AI 仅用于辅助生成知识问答与报告小结草稿，不输出诊断与处方结论，最终结果均由治疗师确认。")

body(doc, "系统测试结果表明，平台在功能、性能与算法三个方面均达到预期目标：三端核心业务流程功能正确、权限边界清晰，未发现越权访问与重复提交问题；核心接口响应时间、并发能力以及端侧推理延迟与内存占用均满足非功能需求；端侧姿态评估在角度测量、动作计数与评分一致性方面达到验收指标，遮挡与置信度不足等异常场景能够被正确识别并降级处理，同一动作版本下对同一份数据的评分可完全复现。")

body(doc, "限于个人的实践经验与专业能力，本文仍存在一些不足。一方面，评估主要基于二维姿态估计，对躯干旋转、三维协调等动作的刻画能力有限，在遮挡严重或拍摄角度不佳时精度也会有所下降；另一方面，动作评估阈值与评分权重主要依据公开资料与有限的人工标注确定，样本规模与康复医学专业校验仍有待加强，系统的真实用户规模与长期使用数据也较为有限。后续工作可以从三个方向继续深入：一是引入三维姿态估计或深度相机作为补充分支，提高对复杂动作的评估能力；二是扩大动作样本，并结合康复治疗师的经验对评估参数进行校准与迭代；三是引入更多可解释的分析与个性化推荐，使平台在辅助治疗决策方面发挥更大的作用。")

# ======================= 致  谢 =======================
doc.add_page_break()
doc.add_paragraph('致  谢', style='Heading 1')

body(doc, "本次毕业设计的顺利完成，标志着我的大学生活即将结束。从课题选题、方案论证到系统开发与论文撰写，我得到了许多老师、同学和家人的帮助与支持，在此谨向所有给予我关心和帮助的人致以最诚挚的谢意。")

body(doc, "首先，感谢学校为我提供了良好的学习环境与丰富的教学资源，让我系统地学习了软件工程的相关知识，为本次毕业设计的开展打下了扎实的基础。特别要感谢我的指导老师【指导教师】，从课题选题、技术路线确定到系统设计与论文修改，老师都给予了耐心细致的指导；老师严谨的治学态度、扎实的专业功底和对工作认真负责的精神，使我受益匪浅。")

body(doc, "感谢在毕业设计过程中给予我帮助的各位老师和同学。当我遇到技术难题与思路困惑时，大家与我一起分析、共同探讨，为我提供了许多实用的思路和建议，让我深刻体会到交流与协作的重要性。")

body(doc, "最后，感谢家人一直以来的理解、支持与鼓励，让我能够安心地投入到学习与毕业设计中；也感谢自己在遇到困难时没有轻易放弃。大学的学习与实践是我人生中宝贵的经历，我将带着本次毕业设计积累的经验与收获，继续在专业道路上不断学习、不断进步。")

# ======================= 参考文献 =======================
doc.add_page_break()
doc.add_paragraph('参考文献', style='Heading 1')

refs = [
    "[1] 霍福华, 韩慧. 基于SpringBoot微服务架构下前后端分离的MVVM模型[J]. 电子技术与软件工程, 2022(1): 4-6.",
    "[2] 王鹏强. 基于Vue的MVVM框架的研究与分析[J]. 电脑知识与技术, 2019, 15(4): 3.",
    "[3] 康乃宸. 基于人体姿态估计的康复训练动作检测方法研究[D]. 郑州: 郑州大学, 2024.",
    "[4] 面向视频的太极拳康复训练评估研究与实现[D]. 成都: 电子科技大学, 2025.",
    "[5] CAO Z, HIDALGO G, SIMON T, et al. OpenPose: realtime multi-person 2D pose estimation using part affinity fields[J]. IEEE Transactions on Pattern Analysis and Machine Intelligence, 2019, 43(1): 172-186.",
    "[6] SUN K, XIAO B, LIU D, et al. Deep high-resolution representation learning for human pose estimation[C]//Proceedings of the IEEE/CVF Conference on Computer Vision and Pattern Recognition (CVPR). Long Beach: IEEE, 2019: 5693-5703.",
    "[7] BAZAREVSKY V, GRISHCHENKO I, RAVEENDRAN K, et al. BlazePose: on-device real-time body pose tracking[J/OL]. arXiv:2006.10204, 2020.",
    "[8] Google. MoveNet: ultra fast and accurate pose detection model[EB/OL]. (2021)[2026-10-09]. https://www.tensorflow.org/hub/tutorials/movenet.",
    "[9] 国家卫生健康委员会. 关于印发加快推进康复医疗工作发展意见的通知: 国卫医发〔2021〕19号[EB/OL]. (2021-06-08)[2026-10-09]. http://www.nhc.gov.cn.",
    "[10] Spring. Spring Boot Reference Documentation[EB/OL]. (2024)[2026-10-09]. https://docs.spring.io/spring-boot/.",
    "[11] Vue.js. Vue 3 Official Documentation[EB/OL]. (2024)[2026-10-09]. https://vuejs.org/.",
    "[12] TensorFlow. TensorFlow.js: Machine Learning for the Web[EB/OL]. (2024)[2026-10-09]. https://www.tensorflow.org/js.",
    "[13] 刘通. 基于多人姿态估计的运动功能评估方法研究[D]. 天津: 河北工业大学, 2023.",
    "[14] 郑奇. 基于计算机视觉的家庭康复训练评估系统研究[D]. 合肥: 中国科学技术大学, 2021.",
    "[15] 郭君毅. 基于计算机视觉的神经发育障碍评估研究与应用[D]. 太原: 中北大学, 2025.",
    "[16] 凌天杰. 基于人体姿态估计算法的康复训练动作评估方法研究与应用[D]. 镇江: 江苏科技大学, 2024.",
    "[17] 高攀登. 基于深度视觉的健身动作识别与规范性评估方法研究[D]. 上海: 上海财经大学, 2024.",
]
for r in refs:
    ref_item(doc, r)

doc.save(DOC)
print('saved:', DOC)
print('paragraphs:', len(doc.paragraphs), 'tables:', len(doc.tables))
