package io.github.yuokada.practice.infrastructure.dynamodb;

import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableSchema;

public class CounterItem {

    public static final TableSchema<CounterItem> TABLE_SCHEMA =
            StaticTableSchema.builder(CounterItem.class)
                    .newItemSupplier(CounterItem::new)
                    .addAttribute(
                            String.class,
                            a ->
                                    a.name("counterName")
                                            .getter(CounterItem::getCounterName)
                                            .setter(CounterItem::setCounterName)
                                            .tags(StaticAttributeTags.primaryPartitionKey()))
                    .addAttribute(
                            Long.class,
                            a ->
                                    a.name("value")
                                            .getter(CounterItem::getValue)
                                            .setter(CounterItem::setValue))
                    .build();

    private String counterName;
    private Long value;

    public CounterItem() {}

    public String getCounterName() {
        return counterName;
    }

    public void setCounterName(String counterName) {
        this.counterName = counterName;
    }

    public Long getValue() {
        return value;
    }

    public void setValue(Long value) {
        this.value = value;
    }
}
