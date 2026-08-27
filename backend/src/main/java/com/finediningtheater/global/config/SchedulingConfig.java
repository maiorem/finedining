package com.finediningtheater.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** 개인정보 3년 보관 후 삭제 스케줄러(협업제안, CLAUDE.md §7.7) 등 배치 작업용. */
@Configuration
@EnableScheduling
public class SchedulingConfig {}
