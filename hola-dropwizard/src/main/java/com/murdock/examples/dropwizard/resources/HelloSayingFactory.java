package com.murdock.examples.dropwizard.resources;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * @author weipeng2k 2017年06月19日 下午19:25:48
 */
public class HelloSayingFactory {
    @NotEmpty
    private String saying;

    @JsonProperty
    public String getSaying() {
        return saying;
    }

    @JsonProperty
    public void setSaying(String saying) {
        this.saying = saying;
    }

}
