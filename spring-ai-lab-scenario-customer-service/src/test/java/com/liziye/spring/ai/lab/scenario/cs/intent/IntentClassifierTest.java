package com.liziye.spring.ai.lab.scenario.cs.intent;

import com.liziye.spring.ai.lab.scenario.cs.CustomerServiceProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 意图分类器单元测试。
 */
class IntentClassifierTest {

    private final CustomerServiceProperties properties = new CustomerServiceProperties();
    private final IntentClassifier classifier = new IntentClassifier(properties);

    @ParameterizedTest(name = "[{index}] {0} → {1}")
    @CsvSource(delimiter = '|', textBlock = """
            投诉: 产品质量太差            | 投诉
            投诉: 我要投诉你们            | 投诉
            投诉: 客服态度太差了          | 投诉
            投诉: 气死我了，我要退款      | 投诉
            咨询: 请问怎么退货            | 咨询
            咨询: 这个产品多少钱          | 咨询
            咨询: 你们支持哪些支付方式    | 咨询
            反馈: 我建议增加一个搜索功能  | 反馈
            反馈: 希望你们能改进配送速度  | 反馈
            售后: 我的订单坏了要维修      | 售后
            售后: 我要退货退款            | 售后
            闲聊: 你好啊                  | 闲聊
            闲聊: 今天天气真好            | 闲聊
            """)
    @DisplayName("意图分类")
    void shouldClassifyIntent(String input, String expectedIntent) {
        IntentClassifier.IntentResult result = classifier.classify(input);
        assertThat(result.getIntent())
                .as("input='%s' should classify as '%s'", input, expectedIntent)
                .isEqualTo(expectedIntent);
    }

    @Test
    @DisplayName("空输入应返回闲聊")
    void shouldReturnChatForEmptyInput() {
        IntentClassifier.IntentResult result = classifier.classify("");
        assertThat(result.getIntent()).isEqualTo("闲聊");
        assertThat(result.getConfidence()).isZero();
    }

    @Test
    @DisplayName("null 输入应返回闲聊")
    void shouldReturnChatForNullInput() {
        IntentClassifier.IntentResult result = classifier.classify(null);
        assertThat(result.getIntent()).isEqualTo("闲聊");
        assertThat(result.getConfidence()).isZero();
    }

    @Test
    @DisplayName("投诉意图置信度应大于 0")
    void complaintShouldHavePositiveConfidence() {
        IntentClassifier.IntentResult result = classifier.classify("质量太差，我要投诉你们！太垃圾了！");
        assertThat(result.getIntent()).isEqualTo("投诉");
        assertThat(result.getConfidence()).isGreaterThanOrEqualTo(0.5);
    }

    @Test
    @DisplayName("系统 Prompt 应包含角色信息")
    void systemPromptShouldContainAgentName() {
        String prompt = properties.buildSystemPrompt();
        assertThat(prompt).contains("小智");
        assertThat(prompt).contains("客户服务");
        assertThat(prompt).contains("投诉");
        assertThat(prompt).contains("咨询");
    }
}
