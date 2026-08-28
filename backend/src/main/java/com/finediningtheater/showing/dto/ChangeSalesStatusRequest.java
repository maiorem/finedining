package com.finediningtheater.showing.dto;

import com.finediningtheater.showing.SalesStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeSalesStatusRequest(@NotNull SalesStatus salesStatus) {}
