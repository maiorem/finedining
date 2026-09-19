package com.finediningtheater.site;

import com.finediningtheater.global.support.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

/** 사이트 전역 설정 한 줄(키-값). 지금은 SITE_PUBLIC(공개 여부) 하나뿐이다. */
@Entity
@Getter
@Table(name = "site_setting")
public class SiteSetting extends BaseTimeEntity {

    @Id
    @Column(name = "setting_key", length = 50)
    private String key;

    @Column(name = "setting_value", nullable = false, length = 200)
    private String value;

    protected SiteSetting() {}

    public SiteSetting(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public void changeValue(String value) {
        this.value = value;
    }
}
