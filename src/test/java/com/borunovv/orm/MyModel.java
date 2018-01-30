package com.borunovv.orm;

/**
 * @author borunovv
 */
@DBEntity(table = "accum")
public class MyModel {

    @DBColumn("id")
    private long id;

    @DBColumn("value")
    private long value;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getValue() {
        return value;
    }

    public void setValue(long value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "MyModel{" +
                "id=" + id +
                ", value=" + value +
                '}';
    }
}
