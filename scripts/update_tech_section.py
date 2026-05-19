from pathlib import Path

from docx import Document


SOURCE = Path("F:/Code/Java/cutting-system/thesis-tech-source.docx")
OUTPUT = Path("F:/Code/Java/cutting-system/thesis-tech-expanded.docx")


def delete_paragraph(paragraph):
    element = paragraph._element
    element.getparent().remove(element)


def copy_format(dst, src):
    dst.style = src.style
    dst.paragraph_format.alignment = src.paragraph_format.alignment
    dst.paragraph_format.first_line_indent = src.paragraph_format.first_line_indent
    dst.paragraph_format.left_indent = src.paragraph_format.left_indent
    dst.paragraph_format.right_indent = src.paragraph_format.right_indent
    dst.paragraph_format.space_before = src.paragraph_format.space_before
    dst.paragraph_format.space_after = src.paragraph_format.space_after
    dst.paragraph_format.line_spacing = src.paragraph_format.line_spacing


doc = Document(SOURCE)
paragraphs = doc.paragraphs

start = next(i for i, p in enumerate(paragraphs) if p.text.strip() == "开发环境与工具")
end = next(i for i, p in enumerate(paragraphs[start + 1 :], start + 1) if p.text.strip() == "系统分析与设计")

body_paragraphs = [p for p in paragraphs[start + 1 : end] if p.text.strip()]
sample = body_paragraphs[0]
target = paragraphs[end]

for paragraph in body_paragraphs:
    delete_paragraph(paragraph)

new_texts = [
    "本系统后端开发语言为Java，运行环境为JDK 17，主要开发框架为Spring Boot 3.x。Spring Boot是在Spring框架基础上形成的快速开发框架，能够通过自动配置、内嵌Web容器和统一配置文件减少传统Java Web项目中的重复配置工作。对于本系统而言，Spring Boot主要用于搭建后端接口服务，负责用户登录、客户管理、板材管理、订单管理、排样算法调用和排样结果保存等功能。系统通过Spring MVC提供RESTful接口，使网页端和小程序端能够以统一方式调用后端业务能力。",
    "数据访问层采用MyBatis-Plus框架。MyBatis-Plus在MyBatis基础上封装了通用CRUD、分页查询、条件构造器和字段自动填充等功能，可以减少基础数据表增删改查代码的编写量。系统中的用户、客户、板材、订单、订单明细、余料和排样结果等业务表均通过实体类、Mapper和Service进行操作，使数据访问逻辑与控制层保持相对独立。数据库采用MySQL 8.0，用于保存系统基础资料、订单数据和排样结果数据，能够满足毕业设计阶段的数据存储和查询需求。",
    "网页端采用Vue3作为主要前端框架。Vue3具有组件化开发、响应式数据绑定和组合式API等特点，适合构建交互较多的管理类页面。本系统网页端基于Vue3、Vite、Vue Router、Pinia、Axios和Element Plus实现。Vite用于提供快速的前端开发和构建能力，Vue Router用于管理页面路由，Pinia用于维护登录状态和业务状态，Axios用于封装HTTP请求，Element Plus用于提供表单、表格、按钮、弹窗等常用界面组件。排样结果展示部分结合Canvas 2D进行绘制，可以直观显示板材边界、柜门板件位置和空间利用情况。",
    "移动端采用微信小程序原生框架进行开发。柜门板材加工场景中，现场录入尺寸、选择客户和快速查看排样结果的需求较为明显，小程序具有使用门槛低、移动端适配好和便于现场操作等特点。小程序端主要包括登录、客户管理、板材管理、算法输入和结果展示等页面，通过统一请求工具与Spring Boot后端接口通信，并在请求头中携带JWT令牌完成身份校验。",
    "系统构建与测试方面，后端使用Maven进行依赖管理和项目构建，使用JUnit 5和MockMvc完成接口及业务逻辑测试。开发过程中使用IntelliJ IDEA进行后端编码和调试，使用微信开发者工具进行小程序页面开发与预览，使用Git进行代码版本管理。通过上述开发环境与工具的配合，系统能够形成从后端接口开发、前端页面实现、算法联调到功能测试的完整开发流程。",
]

for text in new_texts:
    p = target.insert_paragraph_before(text)
    copy_format(p, sample)

doc.save(OUTPUT)
print(OUTPUT)
