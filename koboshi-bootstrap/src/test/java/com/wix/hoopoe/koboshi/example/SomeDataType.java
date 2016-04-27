package com.wix.hoopoe.koboshi.example;

/**
 * @author: ittaiz
 * @since: 6/25/13
 */
public class SomeDataType {

    private String content;

    public SomeDataType(String content) {
        this.content = content;
    }

    public SomeDataType() {
        this("");
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public String toString() {
        return "SomeDataType{" +
                "content='" + content + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SomeDataType that = (SomeDataType) o;

        if (!content.equals(that.content)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return content.hashCode();
    }
}
