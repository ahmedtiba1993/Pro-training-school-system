package com.tiba.pts.core.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Active les taches planifiees (purge des refresh tokens obsoletes).
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {}
