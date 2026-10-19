# -*- coding: utf-8 -*-
"""
生成《基于SpringBoot与MoveNet的康复训练辅助平台的设计与实现》论文初稿（摘要 + 第一、二章）
格式依据《湖北理工学院计算机学院毕业设计（论文）正文格式规范》
"""
import os
from docx import Document
from docx.shared import Pt, Cm, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH, WD_LINE_SPACING
from docx.enum.section import WD_SECTION
from docx.oxml.ns import qn
from docx.oxml import OxmlElement

OUT = r"F:\基于springboot框架MoveNet模型人体姿态评估康复训练辅助平台\论文初稿加开题答辩\毕业论文初稿-基于SpringBoot与MoveNet的康复训练辅助平台.docx"

CN = "宋体"
CN_H = "黑体"
EN = "Times New Roman"


# ---------------- 基础工具 ----------------
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


def style_fonts(style, cn, en, size, bold=None):
    f = style.font
    f.name = en
    f.size = Pt(size)
    if bold is not None:
        f.bold = bold
    f.color.rgb = RGBColor(0, 0, 0)
    rpr = style.element.get_or_add_rPr()
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


def body(doc, text, indent=True, align=WD_ALIGN_PARAGRAPH.JUSTIFY):
    p = doc.add_paragraph(text)
    p.alignment = align
    if indent:
        first_line_indent_chars(p)
    return p


def caption(doc, text):
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.space_before = Pt(6)
    p.paragraph_format.space_after = Pt(3)
    r = p.add_run(text)
    run_font(r, CN, EN, 10.5)
    return p


def figure(doc, path, cap, width_cm=10.5):
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.space_before = Pt(6)
    p.paragraph_format.space_after = Pt(0)
    p.paragraph_format.line_spacing = 1.0  # 单倍行距，避免固定值22磅裁剪内嵌图片
    p.paragraph_format.keep_with_next = True
    p.add_run().add_picture(path, width=Cm(width_cm))
    c = doc.add_paragraph()
    c.alignment = WD_ALIGN_PARAGRAPH.CENTER
    c.paragraph_format.space_before = Pt(3)
    c.paragraph_format.space_after = Pt(6)
    r = c.add_run(cap)
    run_font(r, CN, EN, 10.5)
    return c


# ---------------- 页面设置 ----------------
def setup_section(s):
    s.page_width = Cm(21)
    s.page_height = Cm(29.7)
    s.top_margin = Cm(3.5)
    s.bottom_margin = Cm(2.5)
    s.left_margin = Cm(3.0)
    s.right_margin = Cm(2.4)
    s.header_distance = Cm(2.5)
    s.footer_distance = Cm(1.8)
    sectPr = s._sectPr
    pgMar = sectPr.find(qn('w:pgMar'))
    if pgMar is not None:
        pgMar.set(qn('w:gutter'), '283')  # 0.5cm


def set_page_start(section, n):
    sectPr = section._sectPr
    pg = sectPr.find(qn('w:pgNumType'))
    if pg is None:
        pg = OxmlElement('w:pgNumType')
        sectPr.append(pg)
    pg.set(qn('w:start'), str(n))


def add_page_number_footer(section):
    section.footer.is_linked_to_previous = False
    p = section.footer.paragraphs[0]
    p.alignment = WD_ALIGN_PARAGRAPH.RIGHT
    r = p.add_run()
    f1 = OxmlElement('w:fldChar'); f1.set(qn('w:fldCharType'), 'begin'); r._r.append(f1)
    r2 = p.add_run()
    it = OxmlElement('w:instrText'); it.set(qn('xml:space'), 'preserve'); it.text = ' PAGE '
    r2._r.append(it)
    r3 = p.add_run()
    f2 = OxmlElement('w:fldChar'); f2.set(qn('w:fldCharType'), 'separate'); r3._r.append(f2)
    r4 = p.add_run('1')
    r5 = p.add_run()
    f3 = OxmlElement('w:fldChar'); f3.set(qn('w:fldCharType'), 'end'); r5._r.append(f3)
    for rr in p.runs:
        run_font(rr, CN, EN, 10.5)


def add_toc(doc):
    p = doc.add_paragraph()
    r = p.add_run()
    f1 = OxmlElement('w:fldChar'); f1.set(qn('w:fldCharType'), 'begin'); r._r.append(f1)
    r2 = p.add_run()
    it = OxmlElement('w:instrText'); it.set(qn('xml:space'), 'preserve')
    it.text = ' TOC \\o "1-3" \\h \\z \\u '
    r2._r.append(it)
    r3 = p.add_run()
    f2 = OxmlElement('w:fldChar'); f2.set(qn('w:fldCharType'), 'separate'); r3._r.append(f2)
    r4 = p.add_run('【目录域：请在 Word 中按 Ctrl+A 后按 F9 更新】')
    run_font(r4, CN, EN, 12)
    r5 = p.add_run()
    f3 = OxmlElement('w:fldChar'); f3.set(qn('w:fldCharType'), 'end'); r5._r.append(f3)
    return p


def set_table_h_borders(table):
    tbl = table._tbl
    tblPr = tbl.tblPr
    old = tblPr.find(qn('w:tblBorders'))
    if old is not None:
        tblPr.remove(old)
    borders = OxmlElement('w:tblBorders')
    spec = [('top', 'single', '8'), ('bottom', 'single', '8'), ('insideH', 'single', '6')]
    for tag, val, sz in spec:
        el = OxmlElement('w:' + tag)
        el.set(qn('w:val'), val); el.set(qn('w:sz'), sz)
        el.set(qn('w:space'), '0'); el.set(qn('w:color'), '000000')
        borders.append(el)
    for tag in ('left', 'right', 'insideV'):
        el = OxmlElement('w:' + tag)
        el.set(qn('w:val'), 'none'); el.set(qn('w:sz'), '0'); el.set(qn('w:space'), '0')
        borders.append(el)
    tblPr.append(borders)


# ---------------- 文档 ----------------
doc = Document()

# 样式：正文
st = doc.styles['Normal']
style_fonts(st, CN, EN, 12, bold=False)
st.paragraph_format.line_spacing = Pt(22)
st.paragraph_format.line_spacing_rule = WD_LINE_SPACING.EXACTLY
st.paragraph_format.space_before = Pt(0)
st.paragraph_format.space_after = Pt(0)

# 样式：章标题（一级）
h1 = doc.styles['Heading 1']
style_fonts(h1, CN_H, EN, 16, bold=False)
h1.paragraph_format.alignment = WD_ALIGN_PARAGRAPH.CENTER
h1.paragraph_format.line_spacing = Pt(22)
h1.paragraph_format.line_spacing_rule = WD_LINE_SPACING.EXACTLY
h1.paragraph_format.space_before = Pt(18)
h1.paragraph_format.space_after = Pt(16)

# 样式：节标题（二级）
h2 = doc.styles['Heading 2']
style_fonts(h2, CN_H, EN, 14, bold=False)
h2.paragraph_format.alignment = WD_ALIGN_PARAGRAPH.LEFT
h2.paragraph_format.line_spacing = Pt(22)
h2.paragraph_format.line_spacing_rule = WD_LINE_SPACING.EXACTLY
h2.paragraph_format.space_before = Pt(13)
h2.paragraph_format.space_after = Pt(13)

# 样式：条标题（三级）
h3 = doc.styles['Heading 3']
style_fonts(h3, CN, EN, 12, bold=True)
h3.paragraph_format.alignment = WD_ALIGN_PARAGRAPH.LEFT
h3.paragraph_format.line_spacing = Pt(22)
h3.paragraph_format.line_spacing_rule = WD_LINE_SPACING.EXACTLY
h3.paragraph_format.space_before = Pt(6)
h3.paragraph_format.space_after = Pt(6)

# ---------- 封面 ----------
for _ in range(3):
    doc.add_paragraph()
pt = doc.add_paragraph()
pt.alignment = WD_ALIGN_PARAGRAPH.CENTER
rt = pt.add_run('毕 业 设 计（论文）')
run_font(rt, CN_H, EN, 26, bold=True)
rt.font.underline = True
doc.add_paragraph()
doc.add_paragraph()

COVER = [
    ('题\u3000\u3000目', '基于SpringBoot与MoveNet的康复训练辅助平台的设计与实现'),
    ('学\u3000\u3000院', '计算机学院'),
    ('专业名称', '软件工程'),
    ('学\u3000\u3000号', '202530495223'),
    ('学生姓名', '许袁'),
    ('指导教师', '【指导教师】'),
]
for label, val in COVER:
    p = doc.add_paragraph()
    p.paragraph_format.left_indent = Cm(2.5)
    p.paragraph_format.line_spacing = Pt(30)
    p.paragraph_format.line_spacing_rule = WD_LINE_SPACING.EXACTLY
    r1 = p.add_run(label + '：')
    run_font(r1, CN, EN, 14)
    r2 = p.add_run(val)
    run_font(r2, CN, EN, 14)

doc.add_paragraph()
pdate = doc.add_paragraph()
pdate.alignment = WD_ALIGN_PARAGRAPH.CENTER
rd = pdate.add_run('2026 年 6 月')
run_font(rd, CN, EN, 14)

# ---------- 摘要 ----------
doc.add_page_break()
doc.add_paragraph('摘\u3000\u3000要', style='Heading 1')

ZH = [
    "随着我国人口老龄化进程加快，脑卒中、骨关节损伤及运动损伤等患者的数量持续增加，康复训练的社会需求日益旺盛。康复训练的效果高度依赖动作的规范程度与训练的连续性，而传统康复主要依靠治疗师现场观察并给予口头纠正，存在优质治疗师资源短缺、人力成本高、评价标准主观、居家训练阶段缺乏监督与量化反馈、训练过程难以形成连续可追溯记录等问题，已难以满足患者长期、连续、个性化的康复需求。",
    "针对上述问题，本文设计并实现了一个基于SpringBoot框架与MoveNet模型的人体姿态评估康复训练辅助平台。平台采用前后端分离的B/S架构，并将姿态推理完全下沉到浏览器端执行：用户端基于Vue3构建，通过ONNX Runtime Web在浏览器本地加载轻量级MoveNet单姿态模型，对摄像头视频流逐帧推理得到人体17个关键点，并在端侧依次完成关键点质量门控、时序平滑与遮挡修复、骨架归一化及运动特征计算，再经动作签名校验与阶段状态机实现动作计数，最后从动作幅度、保持时间、节奏、稳定性、对称性和模板相似度六个维度加权评分，并实时给出纠错提示。",
    "服务端基于SpringBoot3框架，结合MyBatis-Plus、MySQL、Redis、JWT与MinIO等技术，实现了账户与权限、动作与算法、康复计划、姿态评估与训练、数据与报告、公共内容与文件、咨询与运维共七大业务域，为患者、治疗师和管理员三类角色提供完整的业务支撑。平台以动作版本快照固化关键点置信度阈值、滤波与修复参数及评分权重，使评估过程可解释、评价结果可追溯复现；同时仅向服务器上传汇总指标与错误摘要，不上传原始视频与逐帧关键点数据，有效保护了患者隐私。",
    "测试结果表明，平台能够在普通设备上稳定完成端侧的实时姿态评估与动作计数，在相同数据与相同动作版本下评分结果保持一致，可为康复训练的量化评价与远程指导提供一种切实可行的解决方案。",
]
for t in ZH:
    body(doc, t)

kw = doc.add_paragraph()
kw.paragraph_format.space_before = Pt(10)
r = kw.add_run('关键词：SpringBoot；MoveNet；人体姿态估计；康复训练；动作评估')
run_font(r, CN, EN, 12, bold=True)

# ---------- Abstract ----------
doc.add_page_break()
doc.add_paragraph('Abstract', style='Heading 1')

EN_ABS = [
    "With the acceleration of population aging in China, the number of patients with stroke, osteoarticular injuries and sports injuries keeps increasing, and the demand for rehabilitation training is growing rapidly. The effect of rehabilitation training depends heavily on the standardization of movements and the continuity of training. Traditional rehabilitation mainly relies on therapists' on-site observation and verbal correction, which suffers from a shortage of qualified therapists, high labor cost, subjective evaluation criteria, the lack of supervision and quantitative feedback during home-based training, and the difficulty in forming continuous and traceable training records. As a result, it can hardly meet the long-term, continuous and personalized rehabilitation needs of patients.",
    "To address these problems, this paper designs and implements a human pose evaluation rehabilitation training assistance platform based on the SpringBoot framework and the MoveNet model. The platform adopts a front-end and back-end separated B/S architecture and moves pose inference entirely to the browser side. The client is built with Vue3 and loads the lightweight MoveNet single-pose model locally in the browser through ONNX Runtime Web, performing frame-by-frame inference on the camera video stream to obtain 17 human keypoints. On the client side, it successively performs keypoint quality gating, temporal smoothing and occlusion repair, skeleton normalization and motion feature computation, then realizes action counting through motion signature verification and a phase state machine, and finally produces a weighted score from six dimensions, namely range of motion, holding time, rhythm, stability, symmetry and template similarity, together with real-time correction prompts.",
    "The server is built on the SpringBoot3 framework and integrates MyBatis-Plus, MySQL, Redis, JWT and MinIO to implement seven business domains, namely account and permission, action and algorithm, rehabilitation plan, pose evaluation and training, data and report, public content and file, and consultation and operation maintenance, providing complete business support for three kinds of roles: patients, therapists and administrators. The platform fixes keypoint confidence thresholds, filtering and repair parameters and scoring weights by action version snapshots, which makes the evaluation process interpretable and the results traceable and reproducible. Moreover, only aggregated indicators and error summaries are uploaded to the server rather than raw videos and frame-level keypoints, so patients' privacy is effectively protected.",
    "Test results show that the platform can stably complete client-side real-time pose evaluation and action counting on ordinary devices, and the scoring results remain consistent under the same data and the same action version, providing a feasible solution for quantitative evaluation and remote guidance of rehabilitation training.",
]
for t in EN_ABS:
    p = doc.add_paragraph(t)
    p.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY
    first_line_indent_chars(p)

kwe = doc.add_paragraph()
kwe.paragraph_format.space_before = Pt(10)
r = kwe.add_run('Key words: SpringBoot; MoveNet; Human Pose Estimation; Rehabilitation Training; Action Evaluation')
run_font(r, EN, EN, 12, bold=True)

# ---------- 目录 ----------
doc.add_page_break()
pd = doc.add_paragraph()
pd.alignment = WD_ALIGN_PARAGRAPH.CENTER
pd.paragraph_format.space_after = Pt(16)
rd = pd.add_run('目\u3000\u3000录')
run_font(rd, CN_H, EN, 16)
add_toc(doc)

# ---------- 正文（新节，页码从 1 开始）----------
sec2 = doc.add_section(WD_SECTION.NEW_PAGE)

# 第一章
doc.add_paragraph('第一章  绪论', style='Heading 1')

doc.add_paragraph('1.1  课题背景', style='Heading 2')
for t in [
    "康复医学是帮助患者恢复运动功能、提高生活自理能力的重要学科。近年来，随着我国人口老龄化进程加快，脑卒中、帕金森病、骨关节损伤及运动损伤等患者的数量持续增长，康复训练的社会需求日益旺盛。国家在《“健康中国2030”规划纲要》中明确提出要加强康复医疗服务体系建设，并鼓励运用互联网、人工智能等信息技术推动康复服务向基层和家庭延伸，远程康复与智能化康复训练逐渐成为研究与应用的热点。",
    "康复训练的核心在于通过反复、规范的动作练习重建神经肌肉控制能力，其效果高度依赖动作的规范程度与训练的连续性。然而，传统康复训练主要由治疗师在现场进行一对一指导，治疗师通过肉眼观察判断患者动作是否到位，并给予口头纠正。这种模式存在明显不足：一是优质康复治疗师资源相对短缺，一对一的指导方式人力成本高、服务半径有限；二是动作评价依赖治疗师的主观经验，缺乏客观量化标准，不同治疗师之间难以保持一致；三是患者离开医院后进入居家训练阶段，训练过程缺乏监督，动作是否正确无从判断，容易形成错误代偿动作，且训练依从性较差；四是训练过程与训练效果难以形成连续、可追溯的数据记录，不利于制定后续康复方案。",
    "人体姿态估计（Human Pose Estimation）技术的发展为解决上述问题提供了新的技术途径。姿态估计旨在从图像或视频中定位人体的关键关节，得到人体骨架表示。随着深度学习技术的成熟，基于卷积神经网络的关键点检测方法在精度和速度上均取得了显著进展，OpenPose、HRNet、BlazePose、MoveNet等模型相继提出，其中面向移动端和浏览器端的轻量级模型可以在普通设备的摄像头视频流上实现实时推理。与此同时，ONNX Runtime Web等端侧推理框架的出现，使得在浏览器中直接加载并运行神经网络模型成为可能，患者无需安装专用软件、无需上传原始视频即可在本地完成动作分析，为居家康复场景下的实时评估与隐私保护提供了工程可行性。",
    "本课题正是在上述背景下提出的。课题结合软件工程专业实践，以“将轻量级姿态估计模型与康复训练业务流程相结合”为出发点，设计并实现一个基于SpringBoot框架与MoveNet模型的人体姿态评估康复训练辅助平台，在浏览器端完成动作的实时量化评估，在服务端完成康复计划、训练记录、评估报告、内容管理与系统运维等业务支撑，形成面向患者、治疗师和管理员三类角色的完整应用系统。",
]:
    body(doc, t)

doc.add_paragraph('1.2  课题意义', style='Heading 2')
for t in [
    "（1）技术意义。本课题将轻量级人体姿态估计模型引入浏览器端康复训练场景，探索了在低算力终端上实现实时动作量化评估的工程实现方法。平台以动作版本为边界组织评估参数，将关键点置信度阈值、滤波与插值参数、状态机参数以及多维评分权重等统一固化在动作版本快照中，使评估过程具有可解释性与可追溯性；同时通过关键点质量门控与修复机制，在数据质量不足时只记录而不计分，避免了低质量数据对评价结果的干扰。这些设计与实现为同类端侧动作评估系统提供了可借鉴的工程方案。",
    "（2）应用价值。平台为三类用户提供了针对性的支持：对患者而言，居家训练时可通过摄像头获得实时的动作评分与纠错提示，训练过程被自动记录并形成趋势数据，训练依从性与规范性得到提升；对治疗师而言，可以基于患者权威动作与计划完成情况制定和调整康复计划，并依据系统生成的训练记录与评估报告进行审核，将有限的精力从重复观察转向方案制定；对管理员而言，可以统一维护标准动作库、评估规则与公共内容，保证全平台评估标准的一致。",
    "（3）隐私与安全价值。平台将姿态推理完全放在浏览器本地执行，服务器只接收汇总指标、错误摘要与质量指标，不上传原始视频及逐帧关键点数据，在满足功能需求的同时最大限度地降低了患者隐私泄露的风险。",
    "（4）综合意义。本课题将姿态估计这一人工智能技术与康复训练这一现实医疗需求结合起来，以软件工程的方法完成需求分析、系统设计、编码实现与测试，既锻炼了工程实践能力，也为康复训练的量化评估与远程指导提供了一种可行的解决方案，具有一定的社会效益与推广价值。",
]:
    body(doc, t)

doc.add_paragraph('1.3  国内外研究现状', style='Heading 2')
doc.add_paragraph('1.3.1  国外研究现状', style='Heading 3')
for t in [
    "国外对人体姿态估计的研究起步较早。早期方法主要依赖可变形部件模型等人工特征，在复杂背景下鲁棒性有限。2017年，Cao等人提出OpenPose，采用自底向上的部位亲和场（Part Affinity Fields）方法，将关键点检测与关键点连接解耦，实现了多人场景下的实时二维姿态估计，成为该领域的里程碑式工作。此后，研究者不断在高分辨率特征保持方面进行改进，2020年Wang等人提出的HRNet通过在整个网络中维持高分辨率特征表示并反复进行多尺度融合，显著提升了关键点定位精度，成为高精度姿态估计的代表性网络。",
    "面向移动端和低算力设备，Google先后提出了BlazePose与MoveNet等轻量化模型。BlazePose通过轻量骨干网络与编码解码结构实现人体关键点检测，被应用于MediaPipe框架中；MoveNet则采用单阶段热力图回归结构，输出17个人体关键点，并提供了侧重速度的Lightning与侧重精度的Thunder两种变体，可在普通设备上达到较高的推理帧率。与此同时，TensorFlow.js、ONNX Runtime Web等端侧推理框架不断成熟，使模型可以直接在浏览器中运行，推动了姿态估计在健身指导、动作评估等交互式场景中的应用。",
    "在康复训练领域，国外研究者较早尝试将姿态估计应用于动作质量评价。部分研究利用Kinect等深度传感器采集三维关节坐标，通过计算关节角度活动范围来评估患者的动作完成度；也有研究采用动态时间规整（DTW）等方法，将患者动作序列与标准示范序列进行对齐和相似度比较，从而给出动作规范性评分；近年来，基于图卷积网络、时序注意力机制的动作识别与评价方法也被引入康复动作分析中，进一步提高了评价的准确性。",
]:
    body(doc, t)

doc.add_paragraph('1.3.2  国内研究现状', style='Heading 3')
for t in [
    "国内在姿态估计与康复评估方面同样开展了大量研究。在算法层面，研究者围绕关键点检测精度、遮挡鲁棒性和动作识别准确率进行了改进。例如，郑州大学2024年的研究《基于人体姿态估计的康复训练动作检测方法研究》以高分辨率二维关键点网络为基础，引入二维到三维姿态提升方法获取空间信息，并针对遮挡问题设计了基于Transformer的前后帧修复机制，同时利用多尺度图卷积网络完成招式识别，通过度量学习对动作的规范程度进行评价，在康复动作检测方面取得了较好的效果。",
    "在应用层面，电子科技大学2025年的研究《面向视频的太极拳康复训练评估研究与实现》面向视频数据，实现了动作的评估与反馈，探索了将姿态估计技术用于传统健身康复项目的可行路径。除此之外，国内还有不少工作将姿态估计与智能康复机器人、居家康复训练平台、体育教学评估等场景相结合，通过计算关节角度、活动范围、动作速度与平稳性等指标对动作质量进行量化分析。",
    "总体来看，国内外研究在姿态估计精度和动作识别方法上已较为深入，为本课题提供了良好的理论与技术基础。但现有研究多聚焦于算法本身，面向真实康复训练场景、能够将端侧实时评估与康复业务流程完整打通的工程化系统相对较少，这为本课题的研究留下了空间。",
]:
    body(doc, t)

doc.add_paragraph('1.4  当前存在的问题', style='Heading 2')
for t in [
    "结合上述研究现状与实际康复训练场景，当前相关系统与研究中仍存在以下问题：",
    "（1）算法部署成本较高。主流高精度姿态估计方法多依赖高分辨率网络与较大算力，通常部署在具有图形处理器的服务器上，对普通家庭环境而言部署与使用门槛偏高，难以满足居家训练的实时性与易用性要求。",
    "（2）业务闭环不完整。多数研究聚焦于单个动作的识别或评分，缺少康复计划制定、训练任务生成、训练记录汇总、评估报告审核等环节的衔接，难以支撑治疗师与患者之间的持续互动。",
    "（3）数据质量缺乏门控。在居家采集条件下，光照变化、遮挡、出画等问题普遍存在，若不加以判别而直接将低质量数据纳入评分，会导致评价结果失真，甚至对患者形成误导。",
    "（4）患者隐私存在隐患。部分方案需要将原始训练视频上传至服务器处理，视频中含有患者的面部与身体特征，一旦泄露将带来隐私风险，患者接受度较低。",
    "（5）评估过程可追溯性不足。许多系统的评分阈值与算法参数固化在代码中，未与动作版本绑定，导致同一套动作在不同时间、不同版本下的评分标准不一致，结果难以复现，也不便于治疗师追溯扣分原因。",
    "（6）评价维度较为单一。仅依据关节角度阈值判断动作是否达标，难以反映动作幅度、节奏、稳定性和对称性等方面的差异，评价结果的区分度有限。",
    "针对以上问题，本课题选择在浏览器端部署轻量级单姿态估计模型，结合质量门控与多维评分方法实现动作的实时量化评估，并以动作版本快照保证参数可追溯、结果可复现，同时构建覆盖患者、治疗师与管理员三类角色的完整业务系统。",
]:
    body(doc, t)

doc.add_paragraph('1.5  论文组织结构', style='Heading 2')
for t in [
    "本文共分为六章，各章内容安排如下：",
    "第一章 绪论。介绍课题的研究背景、研究意义、国内外研究现状以及当前存在的主要问题，并说明本文的组织结构。",
    "第二章 关键技术及工具简介。介绍系统开发所采用的开发环境与工具，以及前端、后端、数据库与中间件和人体姿态估计与端侧推理等关键技术。",
    "第三章 系统分析。对系统进行可行性分析，并从功能需求与非功能需求两方面进行需求分析，给出系统角色、用例与业务流程。",
    "第四章 系统设计。阐述系统的总体架构设计、功能模块设计与数据库设计，给出系统架构图、功能模块图、业务流程图以及数据库概念结构与数据表设计。",
    "第五章 系统实现。按功能模块介绍系统的具体实现，给出主要功能的界面效果与前后端关键代码。",
    "第六章 系统测试。对系统进行功能测试与性能测试，并对测试结果进行分析与总结。",
    "最后为结论、致谢与参考文献等部分。",
]:
    body(doc, t)

# 第二章（另起一页）
doc.add_page_break()
doc.add_paragraph('第二章  关键技术及工具简介', style='Heading 1')

doc.add_paragraph('2.1  系统开发环境与工具', style='Heading 2')
for t in [
    "本平台采用前后端分离的B/S架构进行开发，后端以Java语言为主，前端以TypeScript语言为主，开发与运行过程中使用的操作系统、开发工具与运行环境如表2-1所示。开发阶段使用Windows操作系统与集成开发环境完成编码与调试，部署阶段将后端服务、数据库、缓存、对象存储与Web服务器运行于Linux环境，并通过Docker Compose进行统一编排，以降低环境差异带来的部署成本。",
]:
    body(doc, t)

caption(doc, '表2-1  系统开发环境与工具')
tbl = doc.add_table(rows=11, cols=2)
tbl.autofit = True
DATA = [
    ('类别', '名称与版本'),
    ('操作系统', 'Windows 11（开发）、Linux（部署）'),
    ('开发工具', 'IntelliJ IDEA、Visual Studio Code'),
    ('后端运行环境', 'JDK 17、Maven 3.9'),
    ('前端运行环境', 'Node.js 18、Vite 5'),
    ('数据库', 'MySQL 8.0'),
    ('缓存', 'Redis 7.2'),
    ('对象存储', 'MinIO'),
    ('Web服务器', 'Nginx'),
    ('容器化工具', 'Docker、Docker Compose'),
    ('版本管理', 'Git'),
]
for i, (a, b) in enumerate(DATA):
    for j, val in enumerate((a, b)):
        cell = tbl.cell(i, j)
        cell.text = ''
        p = cell.paragraphs[0]
        p.alignment = WD_ALIGN_PARAGRAPH.CENTER if i == 0 else WD_ALIGN_PARAGRAPH.LEFT
        p.paragraph_format.line_spacing = Pt(20)
        p.paragraph_format.line_spacing_rule = WD_LINE_SPACING.EXACTLY
        p.paragraph_format.space_before = Pt(1)
        p.paragraph_format.space_after = Pt(1)
        r = p.add_run(val)
        run_font(r, CN, EN, 10.5, bold=(i == 0))
set_table_h_borders(tbl)

doc.add_paragraph('2.2  前端相关技术', style='Heading 2')
doc.add_paragraph('2.2.1  Vue.js 与 Vite', style='Heading 3')
for t in [
    "Vue.js是一款用于构建用户界面的渐进式JavaScript框架。本平台前端采用Vue 3.4版本，其组合式API（Composition API）允许将同一业务逻辑的状态、计算属性与副作用组织在一起，便于在姿态评估这类逻辑复杂的页面中进行代码复用与维护；响应式系统能够在数据变化时自动更新视图，减少了手动操作DOM的工作量；组件化机制则将页面拆分为可复用的独立单元，提高了开发效率与代码的可读性。",
    "Vite是新一代前端构建工具，开发环境下基于浏览器原生ES模块加载源码，配合依赖预构建实现了快速的冷启动与热更新；生产环境下基于Rollup进行打包，输出体积较小的静态资源。本平台管理端与用户端均使用Vite 5进行构建，并通过TypeScript提供静态类型检查，在编译阶段即可发现类型错误，提高了代码的健壮性。",
]:
    body(doc, t)

doc.add_paragraph('2.2.2  Element Plus 与 Vant 组件库', style='Heading 3')
for t in [
    "Element Plus是一套基于Vue 3的桌面端组件库，提供了表格、表单、分页、对话框、级联选择等丰富的组件，能够较好地满足管理端大量数据录入与展示的需求。本平台管理端使用Element Plus构建动作库维护、评估规则配置、内容审核与系统运维等界面。",
    "Vant是一套轻量、可靠的移动端组件库，组件风格适配移动设备，交互方式贴近手机使用习惯。本平台用户端同时承载患者端与治疗师端，其计划查看、训练任务、训练记录与咨询等页面使用Vant组件库构建，以保证在手机浏览器上的显示与操作体验。两个前端工程均通过自动导入插件按需引入组件与样式，避免全量引入造成资源浪费。",
]:
    body(doc, t)

doc.add_paragraph('2.2.3  Pinia、Vue Router 与 Axios', style='Heading 3')
for t in [
    "Pinia是Vue的官方状态管理库，以store的形式集中管理跨组件共享的状态。本平台使用Pinia保存用户登录信息、权限角色以及训练过程中的会话状态，便于页面之间共享数据。",
    "Vue Router是Vue官方提供的路由管理器。本平台用户端在用户登录后依据角色将请求分发到患者端或治疗师端路由，并通过全局前置守卫对未登录或无权限的访问进行拦截，实现基于角色的页面访问控制。",
    "Axios是一个基于Promise的HTTP客户端，用于在浏览器中发起异步请求。本平台对Axios进行统一封装，在请求拦截器中自动为请求添加认证令牌，在响应拦截器中统一处理业务状态码与登录失效，简化了各业务模块中的接口调用代码。此外，平台使用ECharts对训练数据的统计结果进行可视化展示。",
]:
    body(doc, t)

doc.add_paragraph('2.3  后端相关技术', style='Heading 2')
doc.add_paragraph('2.3.1  Spring Boot', style='Heading 3')
for t in [
    "Spring Boot是基于Spring框架的快速开发框架，通过自动配置与起步依赖显著简化了企业级应用的搭建过程。本平台后端基于Spring Boot 3.2.5构建，运行于JDK 17环境，使用内嵌Tomcat作为Web服务器，采用Jakarta EE命名空间。Spring Boot的自动配置机制根据引入的依赖自动装配数据源、事务管理器、缓存等组件，开发者只需编写业务代码即可；统一的配置文件管理则支持按环境（开发、生产）切换配置，便于部署。",
    "在工程结构上，后端按照公共基础、领域模型与接口层进行模块化划分：公共基础模块提供统一响应结构、全局异常处理、通用工具与常量定义；领域模型模块定义实体、数据传输对象与视图对象；接口层则按面向用户端与管理端分别组织控制器、服务与数据访问代码，使不同角色的接口边界清晰，便于维护与权限控制。",
]:
    body(doc, t)

doc.add_paragraph('2.3.2  MyBatis-Plus', style='Heading 3')
for t in [
    "MyBatis-Plus是MyBatis的增强工具，在不改变MyBatis原有功能的前提下提供了大量便捷特性。本平台使用MyBatis-Plus 3.5.7作为持久层框架，其通用Mapper为常见的增删改查操作提供了开箱即用的实现，减少了重复的SQL编写；条件构造器以链式调用的方式动态拼接查询条件，适合处理多条件组合查询；分页插件则通过拦截器自动完成分页查询，简化了列表页面的实现。",
    "此外，平台通过全局配置开启了驼峰与下划线字段的自动映射，并统一约定了逻辑删除字段，使带有逻辑删除标记的数据查询自动过滤已删除记录，同时在配置中关闭并简化了部分提示输出，保持了日志的整洁。",
]:
    body(doc, t)

doc.add_paragraph('2.3.3  安全与认证相关技术', style='Heading 3')
for t in [
    "JWT（JSON Web Token）是一种紧凑的、自包含的令牌格式，适用于前后端分离架构下的无状态认证。本平台使用JJWT 0.12.6生成与解析令牌，用户登录成功后由服务端签发访问令牌，前端在后续请求中通过请求头携带该令牌，服务端在拦截器中校验令牌的合法性并解析出用户身份与角色信息，从而完成鉴权。访问令牌设有有效期，到期后需要重新登录，降低了令牌泄露带来的风险。",
    "Redis用于存储登录会话、登录失败计数与锁定状态、幂等控制以及热点数据缓存。当用户在短时间内连续输错密码达到设定次数时，系统将其账号临时锁定，此举可有效缓解口令暴力破解；对同一请求的重复提交则通过幂等键进行拦截，避免重复写入。密码在存储前使用BCrypt算法进行散列，服务端仅保存散列值，不保存明文。为提升登录环节的安全性，平台还使用EasyCaptcha生成图形验证码，并要求在登录时进行校验。",
]:
    body(doc, t)

doc.add_paragraph('2.3.4  接口文档与工具库', style='Heading 3')
for t in [
    "Knife4j是一套基于OpenAPI 3规范的接口文档增强工具。本平台后端集成Knife4j 4.5.0，通过注解为接口补充描述信息，启动后即可访问在线接口文档页面，便于前后端联调与接口自测。",
    "在工具库方面，平台引入Hutool工具集，用于字符串处理、日期处理、加密与二维码等常见操作，减少重复代码。数据库结构变更通过Flyway进行版本化迁移管理，每次变更以带有版本号的SQL脚本形式纳入工程，服务启动时自动按顺序执行未应用的脚本，保证各环境数据库结构的一致性与可追溯性。数据源使用HikariCP连接池管理数据库连接，通过设置连接有效性检测、最大连接数与空闲连接数等参数，提高数据库访问的稳定性。",
]:
    body(doc, t)

doc.add_paragraph('2.4  数据库与中间件', style='Heading 2')
doc.add_paragraph('2.4.1  MySQL', style='Heading 3')
for t in [
    "MySQL是目前应用广泛的开源关系型数据库管理系统。本平台使用MySQL 8.0作为业务数据库，采用InnoDB存储引擎，支持事务与外键约束，字符集使用utf8mb4以完整支持中文与表情符号。数据库中的表按照业务域进行组织，涵盖用户与关系、动作与算法、康复计划与任务、训练记录与评分、评估报告、公共内容与文件、咨询与运维等，共约二十七张表，各表之间通过主外键与业务字段关联，保证数据的完整性与一致性。数据库表结构的设计将在第四章详细说明。",
]:
    body(doc, t)

doc.add_paragraph('2.4.2  Redis', style='Heading 3')
for t in [
    "Redis是一种基于内存的高性能键值数据库，常用于缓存、会话存储与计数统计等场景。本平台使用Redis 7.2，主要承担三方面职责：一是保存登录会话与令牌相关状态，实现无状态鉴权下的会话控制；二是记录登录失败次数与锁定状态，为账号保护策略提供支撑；三是作为热点数据的缓存与幂等键的存储，减轻数据库压力并防止重复提交。Redis开启追加日志持久化，在服务重启后仍可保留关键状态数据。",
]:
    body(doc, t)

doc.add_paragraph('2.4.3  MinIO', style='Heading 3')
for t in [
    "MinIO是一款兼容Amazon S3协议的高性能对象存储服务，适合存储图片、视频与附件等非结构化数据。本平台的公共内容附件、动作演示素材以及咨询中的图片等文件统一存放于MinIO，数据库中仅保存文件的对象键与元信息，读取时由服务端生成具有时效性的预签名访问地址，前端据此直接访问文件。这种设计避免了将大文件存入数据库，也便于后续对存储容量进行独立扩展。",
]:
    body(doc, t)

doc.add_paragraph('2.5  人体姿态估计与端侧推理技术', style='Heading 2')
doc.add_paragraph('2.5.1  MoveNet 人体姿态估计模型', style='Heading 3')
for t in [
    "MoveNet是Google提出的一种轻量级单人姿态估计模型，采用单阶段的热力图回归结构，直接预测人体关键点的位置与置信度，输出包括鼻、左右眼、左右耳、左右肩、左右肘、左右腕、左右髋、左右膝、左右踝在内的17个关键点。该模型提供了侧重推理速度的Lightning与侧重检测精度的Thunder两种变体：前者结构更小、速度更快，适合在算力有限的设备上运行；后者精度更高，但计算量相对较大。由于本平台面向居家场景，需要在普通计算机或移动设备的浏览器中实现实时推理，因此选用Lightning变体，在保证关键点定位满足关节活动度评估需求的前提下获得较高的处理帧率。",
    "相较于需要多人关键点匹配的自底向上方法，单人模型无需进行关键点分组，后处理流程更为简洁；同时其17个关键点的定义覆盖了康复训练中常用的肩、肘、腕、髋、膝、踝等主要关节，能够满足上肢与下肢关节活动度、对称性等指标的测算需求。平台在训练开始前进行单人入场质量检查并对目标进行跟踪锁定，以保证模型始终针对单一训练者进行估计。",
]:
    body(doc, t)

doc.add_paragraph('2.5.2  ONNX Runtime Web 与跨源隔离', style='Heading 3')
for t in [
    "ONNX Runtime Web是ONNX Runtime的Web版本，能够在浏览器中加载并执行ONNX格式的神经网络模型，支持WebAssembly与WebGL等多种后端。本平台将MoveNet Lightning模型导出为ONNX格式，通过ONNX Runtime Web在用户端浏览器中加载，结合摄像头视频流逐帧完成关键点推理，实现原始视频不出本地的端侧计算。为提升推理性能，平台使用带SIMD指令的多线程WebAssembly版本。",
    "多线程WebAssembly依赖跨源隔离环境才能使用共享内存，因此平台在部署时通过Nginx为用户端页面设置跨源隔离响应头，使页面进入跨源隔离状态；在开发环境中则由构建工具注入相同的响应头。当运行环境不支持跨源隔离时，姿态引擎会自动降级为单线程推理，功能仍然可用，仅帧率有所下降。此外，模型文件与运行时文件均以本地静态资源的方式随前端产物一同分发，并按内容哈希进行版本化与长缓存，运行时只允许加载同源资源，不使用任何远端模型地址，以保证评估过程的可控性与隐私安全。由于浏览器对摄像头访问的限制，用户端页面需要在HTTPS或本地访问环境下运行，并由用户显式授权后方可开始训练。",
]:
    body(doc, t)

# 第三章（另起一页）
doc.add_page_break()
doc.add_paragraph('第三章  系统分析', style='Heading 1')

IMG = r"F:\基于springboot框架MoveNet模型人体姿态评估康复训练辅助平台\论文初稿加开题答辩\毕业论文插图"

doc.add_paragraph('3.1  系统可行性分析', style='Heading 2')
doc.add_paragraph('3.1.1  技术可行性', style='Heading 3')
for t in [
    "本系统后端采用Spring Boot 3.2.5与JDK 17进行开发，持久层使用MyBatis-Plus，数据存储采用MySQL 8.0，缓存采用Redis 7.2，文件存储采用MinIO；前端基于Vue 3.4与Vite 5构建，使用TypeScript开发。上述技术均为当前主流的开源技术，生态体系成熟、社区资料丰富，遇到的问题大多可以找到成熟的解决方案，能够有效保障开发进度与系统质量。",
    "本课题在技术上需要重点解决的问题，是如何在普通家用设备的浏览器中完成人体姿态的实时估计。针对这一问题，平台选用Google提出的轻量级单人姿态估计模型MoveNet Lightning，并将其转换为ONNX格式（模型文件约9MB），通过ONNX Runtime Web在浏览器本地加载运行。该模型输出的17个关键点覆盖了康复训练中常用的肩、肘、腕、髋、膝、踝等主要关节，能够满足关节活动度与动作规范性评估的需求；结合带SIMD指令的多线程WebAssembly后端，可在普通设备上获得较高的推理帧率。多线程运行所需的跨源隔离环境可通过Nginx响应头配置实现，且在环境不支持时可自动降级为单线程推理，功能不受影响。摄像头访问由浏览器原生接口提供，用户授权后即可使用。",
    "此外，系统涉及的业务功能以常见的增删改查、流程审批与数据统计为主，均可以基于现有框架的标准能力实现。课题开发人员具备Java与前端开发的实践经验，前期已围绕姿态估计模型完成了技术验证。综上所述，本系统在技术上是可行的。",
]:
    body(doc, t)

doc.add_paragraph('3.1.2  操作可行性', style='Heading 3')
for t in [
    "系统采用B/S架构，用户无需安装任何客户端软件，通过浏览器即可访问。康复患者端使用移动端组件库构建，界面适配手机屏幕，患者只需一部带摄像头的手机或电脑，授权摄像头后即可开始训练，使用门槛低。训练过程中，系统在开始前会对入场质量进行检查并提示站位调整，训练中对不合格动作实时给出纠错提示，患者无需掌握任何专业知识即可完成训练。",
    "治疗师端与管理端使用桌面端组件库构建，界面以列表、表单与图表为主，布局方式符合管理类系统的使用习惯。系统按角色划分功能入口，不同角色登录后仅能看到其职责范围内的功能，避免了功能干扰。当用户操作有误或提交的数据不符合要求时，系统给出明确的错误提示与处理建议。因此，本系统在操作上是可行的。",
]:
    body(doc, t)

doc.add_paragraph('3.1.3  经济可行性', style='Heading 3')
for t in [
    "系统所采用的开发框架、数据库、缓存、对象存储以及姿态估计模型均为开源或免费提供，不产生软件许可费用。姿态推理在用户浏览器端完成，对服务器没有图形处理器要求，一台普通配置的云服务器即可承载Web服务、数据库、缓存与对象存储，并通过Docker Compose统一编排部署，硬件与运维成本较低。",
    "从使用价值来看，平台能够自动完成动作观察、计数与评分，将治疗师从重复性的现场观察工作中解放出来，使其能够同时服务更多患者，提高了康复服务的人力效率；对患者而言，居家训练即可获得量化反馈，减少了往返医院的交通与时间成本。综上，本系统的建设与运行成本可控，具有较好的经济可行性。",
]:
    body(doc, t)

doc.add_paragraph('3.2  系统需求分析', style='Heading 2')
doc.add_paragraph('3.2.1  系统功能需求分析', style='Heading 3')

doc.add_paragraph('① 系统用户分析', style='Heading 3')
for t in [
    "根据康复训练业务的实际参与情况，系统划分为康复患者、康复治疗师和系统管理员三类角色，角色固定，不支持动态配置其他角色。",
    "康复患者是训练的执行者。患者账号由管理员统一建立，并由管理员建立患者与治疗师的绑定关系；只有在存在有效主责治疗师的情况下，患者才能查看康复计划、执行训练任务并发起人工咨询。患者可以维护个人的姓名、性别、出生日期、身高体重、康复部位、病史与过敏禁忌等资料，查看本人的训练记录与已生效的评估报告，浏览与收藏公共康复内容；在没有绑定治疗师的情况下，仍可学习公共内容并使用AI康复问答，但AI仅提供一般性康复知识，不做诊断与处方建议。",
    "康复治疗师是康复方案的组织者。治疗师通过工作台管理其负责的患者，为患者制定康复计划并跟踪执行情况，查看训练数据、审核评估报告并回复患者咨询。系统对治疗师实行对象级权限控制：治疗师只能操作当前与自己存在有效绑定关系的患者；绑定解除后，系统不再向其推送该患者的新数据，历史数据仅保留只读访问。",
    "系统管理员是平台的运维者。管理员负责患者与治疗师账号的建立、启用、禁用与密码重置，维护患者与治疗师之间的绑定关系，维护标准动作库与评估规则，审核公共内容，发布公告，并进行操作日志审计与数据备份等系统运维工作。管理员不参与康复医疗结论的制定，默认不接触患者的病史明细与训练原始视频，以保证职责边界清晰、降低隐私风险。",
]:
    body(doc, t)

doc.add_paragraph('② 系统用例图', style='Heading 3')
for t in [
    "用例图用于描述参与者与系统之间的交互关系。本系统三类角色各自独立使用一端，因此分别绘制患者端、治疗师端与管理端用例图。其中，执行实时训练、制定康复计划等核心用例包含若干必须执行的子用例，在图中以include关系表示。",
    "康复患者端的用例如图3-1所示。患者可以注册登录、查看康复计划、执行实时训练、查看训练记录、学习康复内容、在线咨询以及进行AI康复问答。其中，执行实时训练是核心用例，它必然包含动作评分、纠错提示与自动计数三个子用例：动作评分依据动作版本中配置的评估规则对动作进行量化打分，关键点质量不达标的数据不参与评分；纠错提示在检测到动作偏差时给出纠正建议；自动计数则对重复性动作自动统计完成次数。",
]:
    body(doc, t)
figure(doc, os.path.join(IMG, '图3-1 康复患者端用例图.png'), '图3-1  康复患者端用例图', 10.5)
for t in [
    "康复治疗师端的用例如图3-2所示。治疗师可以注册登录、管理负责患者、制定康复计划、查看训练数据、审核评估报告、回复患者咨询以及查看公告。其中，制定康复计划包含参数越限校验子用例，系统在计划发布前校验组数、次数、幅度、时长等参数是否超出动作版本允许的范围；审核评估报告包含AI报告草稿辅助子用例，由系统预生成报告草稿供治疗师参考；回复患者咨询包含AI回复草稿辅助子用例，AI生成的回复草稿必须经治疗师确认后才能发送给患者。",
]:
    body(doc, t)
figure(doc, os.path.join(IMG, '图3-2 康复治疗师端用例图.png'), '图3-2  康复治疗师端用例图', 10.5)
for t in [
    "系统管理端的用例如图3-3所示。管理员可以注册登录、管理账号与绑定关系、维护动作库与规则、审核公共内容、发布公告以及进行系统运维。其中，维护动作库与规则包含配置角度阈值子用例，管理员为每个动作版本设置关键点定义、角度计算公式、角度上下限、评分权重与纠错文案；审核公共内容包含通过、驳回与下架子用例，驳回时必须填写理由；系统运维包含查看操作日志子用例，用于记录与追溯关键业务操作。",
]:
    body(doc, t)
figure(doc, os.path.join(IMG, '图3-3 系统管理端用例图.png'), '图3-3  系统管理端用例图', 9.5)

doc.add_paragraph('③ 系统功能分析', style='Heading 3')
for t in [
    "在上述角色与用例分析的基础上，结合康复训练业务的实际流程，系统按照业务职责划分为七个功能域，各功能域及其主要功能如下。",
    "1．账户与权限域",
    "负责三类角色的账号体系与访问控制，主要包括：注册登录、图形验证码校验、密码修改、个人资料维护、登录记录查询；管理员对患者与治疗师账号的建立、启用、禁用与密码重置；患者与治疗师之间主责与协助绑定关系的建立、变更与解除，同一患者在同一时间最多存在一个有效主责治疗师。",
    "2．动作与算法域",
    "负责姿态评估所依赖的基准数据，主要包括：标准动作主数据的维护、动作版本的发布与管理（动作版本一经发布不可修改，只能通过发布新版本进行调整）、评估规则的配置（关键点定义、角度计算公式、角度上下限、评分权重与纠错文案）、标准动作模板与采样数据的管理。评估规则的修改必须通过新版本进行，保证历史训练数据所参照的评估标准不被篡改。",
    "3．康复计划域",
    "负责康复计划的制定与执行调度，主要包括：治疗师为负责患者制定康复计划，选择动作与版本并配置组数、次数、幅度、时长、休息与训练频率等参数；发布前的参数越限校验；计划的发布、暂停、结束与作废；计划版本的创建与管理；系统按计划调度自动生成每日训练任务，并为患者提供今日训练任务列表与完成度统计。",
    "4．姿态评估与训练域",
    "本系统的核心功能域，负责训练过程中的实时姿态评估，主要包括：摄像头视频流采集与授权管理；基于MoveNet模型的端侧关键点推理；关键点质量门控（置信度低于阈值的数据只记录不计分）；时序平滑与遮挡修复；骨架归一化与关节角度、活动范围、角速度、对称性等运动特征计算；动作签名校验与阶段状态机驱动的动作计数；幅度、保持、节奏、稳定性、对称性与模板相似度六个维度的加权评分；实时纠错提示的生成与展示；训练结束后的汇总数据上报。",
    "5．数据与报告域",
    "负责训练数据的汇总分析与评估报告管理，主要包括：按日期、计划与动作维度查询训练记录；训练时长、次数、达标率与得分趋势的统计分析；任务与计划完成度的三级统计；评估报告的定时预生成（草稿状态）、治疗师补充结论与审核、报告状态流转（草稿、待审核、已生效）以及生效报告对患者可见的控制。",
    "6．公共内容与文件域",
    "负责平台公共康复知识的管理，主要包括：标准动作演示与健康科普等内容的管理与审核（患者仅可查看审核通过且处于有效期内的内容）、按康复部位与分类的筛选、关键词搜索、内容收藏；内容附件与动作演示素材等文件的对象存储管理，读取时通过预签名地址进行访问。",
    "7．咨询与运维域",
    "负责医患沟通与平台基础运维，主要包括：基于绑定关系的人工在线咨询（支持文字与图片，会话关闭后转为只读）、AI辅助回复草稿（确认后方可发送）、未读消息提醒；平台公告的发布与按范围可见控制；关键业务操作日志的记录与查询；数据备份与恢复（恢复操作需二次确认）；康复部位字典等基础数据的维护。",
    "在上述七个功能域之外，系统还提供认证授权、缓存与限流、定时任务调度以及AI辅助等公共支撑能力，以横切方式服务于各业务域。",
]:
    body(doc, t)

doc.add_paragraph('④ 系统业务流程分析', style='Heading 3')
for t in [
    "系统的主业务流程围绕康复计划的制定、执行、评估与调整形成闭环，具体过程如下：管理员首先为患者与治疗师建立账号，并建立两者之间的主责绑定关系；治疗师根据患者情况制定康复计划，选择动作版本并配置训练参数，经参数校验后发布；系统按计划的训练频率自动生成每日训练任务；患者在今日训练列表中选择任务，授权摄像头后进行实时姿态评估与训练，系统逐帧完成关键点推理、动作计数与评分，并给出纠错提示，训练结束后将汇总数据上报；系统据此更新任务进度并生成训练记录，按计划周期预生成评估报告草稿；治疗师查看训练数据与报告草稿，补充结论并审核，报告生效后患者即可查看；治疗师再根据评估结论对计划进行调整，形成新的计划版本，进入下一轮训练循环。",
    "除主流程外，系统还包含两条相对独立的支线：一是公共内容支线，管理员维护并审核公共康复内容，审核通过后患者即可浏览、搜索与收藏，该支线不依赖患者与治疗师的绑定关系；二是在线咨询支线，患者在存在有效绑定关系的前提下发起咨询，治疗师回复并可借助AI草稿辅助，会话关闭后双方只读。各功能模块内部的详细处理流程将在第四章的功能模块设计中结合流程图进行说明。",
]:
    body(doc, t)

doc.add_paragraph('3.2.2  系统非功能需求分析', style='Heading 3')
for t in [
    "（1）性能需求。常规列表与详情接口在正常负载下的响应时间应控制在0.5秒以内，统计查询类接口不超过2秒；系统应支持不少于200名用户同时在线使用。在端侧，姿态估计的单帧推理应保证足够的处理帧率，使评分与纠错提示能够实时呈现，不产生明显滞后；评估报告的定时生成任务应安排在业务低峰期执行，避免影响在线业务。",
    "（2）安全需求。用户密码采用BCrypt算法散列后存储，不保存明文；登录环节引入图形验证码，连续输错密码达到5次时将账号锁定30分钟；系统采用基于角色的访问控制，并在其基础上进行对象级归属校验，确保治疗师只能访问自己负责的患者数据；使用JWT令牌进行无状态认证，令牌设有有效期，账号被禁用后令牌立即失效。平台不采集与上传训练原始视频及逐帧关键点数据，仅上传汇总指标、错误摘要与质量指标；关键业务操作写入操作日志，便于安全审计与问题追溯；数据传输采用HTTPS加密。",
    "（3）易用性需求。三类角色分别使用独立的界面，患者端针对移动设备适配，重要功能入口清晰直观；训练开始前提供站位与入场质量引导，训练中对偏差动作实时给出通俗易懂的纠错提示；咨询与审核等环节提供未读提醒；当操作出错或数据校验不通过时，给出明确的错误原因与处理建议。",
    "（4）可靠性需求。在居家采集条件下，光照变化、遮挡与出画等情况难以避免，系统通过关键点质量门控机制在数据质量不足时只记录不计分，避免低质量数据干扰评价结果；模型加载或推理异常时给出明确提示并支持重试，不影响其他功能使用。系统提供数据备份与恢复能力，定期备份业务数据，恢复操作需二次确认；系统全年可用性不低于99.5%。",
    "（5）可扩展性需求。系统采用分层与模块化设计，公共基础、领域模型与接口层职责清晰，功能域之间低耦合高内聚，便于后续维护与扩展；接口遵循OpenAPI规范并配套在线文档，便于与第三方系统集成；评估参数以动作版本为单位组织，新增动作或调整评估规则无需修改评估代码；模型文件与业务逻辑解耦，后续可替换为精度更高的姿态估计模型而不影响整体架构。",
]:
    body(doc, t)

# ---------- 节属性 ----------
setup_section(doc.sections[0])
setup_section(sec2)
set_page_start(sec2, 1)
add_page_number_footer(sec2)

os.makedirs(os.path.dirname(OUT), exist_ok=True)
doc.save(OUT)

zh_len = sum(len(t) for t in ZH)
en_len = sum(len(t) for t in EN_ABS)
import re as _re
zh_text = ''.join(ZH)
cjk = len(_re.findall(r'[\u4e00-\u9fff]', zh_text))
latin = len(_re.findall(r'[A-Za-z0-9][A-Za-z0-9\-\.]*', zh_text))
print('saved:', OUT)
print('中文摘要 字符数:', zh_len, '| 汉字数:', cjk, '| 英数词条数:', latin, '| Word式字数(汉字+词条):', cjk + latin)
print('英文摘要字符数:', en_len)
print('sections:', len(doc.sections))
