# Thesis Writing Style Rules

All thesis modification content in `docs/thesis/` and related output must follow this style.

## Voice

Write like a non-native Chinese speaker earnestly attempting academic prose. The tone is sincere but linguistically imperfect.

## Do

- Let sentence structure be slightly awkward occasionally, as long as it remains understandable
- Allow imprecise word choice that still conveys the intended meaning
- Include some repetition and verbosity
- Use connective words stiffly (frequent 所以/因此)
- Allow slightly abrupt transitions between paragraphs
- Maintain standard academic terminology (本文/该系统/该方法)
- Explain technical concepts clearly but in a slightly clumsy way
- English technical terms (Three.js, JWT, Vue, Spring Boot etc.) must have no spaces on either side when embedded in Chinese text

## Don't

- Write polished, fluent AI-perfect text
- Write like a native Chinese speaker's natural prose
- Make text incomprehensible for the sake of being "awkward"
- Use colloquial or internet slang
- Use complex rhetorical devices

## 小程序功能描述规则

论文中涉及小程序端功能时，按以下口径描述（与当前代码实现可能不同，以本规则为准）：

- 小程序端用于展示任务进度，功能与网页端生产看板类似
- 生产员通过小程序查看分配给自己的生产任务
- 组织管理员和操作员通过小程序查看任务进度统计

## 修改原则

- 不要太啰嗦，点到为止，对原文的修改不要太多，主要目的是增加新内容
- 保留原文已有的段落和表述，只在需要补充新功能/新技术的地方插入新段落
- 不要重写原文中已经正确的内容，除非原文与当前系统事实明显冲突

## 排样算法描述规则

排样算法相关章节以当前论文文档中已有的内容为准，不做大幅改写。项目代码中算法改动较大，但论文侧保持原文描述即可。

## Output Formatting

- Output txt files: no blank lines between paragraphs, each paragraph directly follows the previous one
- Output txt files: no first-line indentation (do not start paragraphs with `　　` or any other indentation characters)
