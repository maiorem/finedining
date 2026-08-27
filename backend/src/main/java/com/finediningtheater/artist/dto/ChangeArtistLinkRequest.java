package com.finediningtheater.artist.dto;

import jakarta.validation.constraints.Size;

public record ChangeArtistLinkRequest(@Size(max = 500) String linkUrl) {}
