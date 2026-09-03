package com.finediningtheater.inquiry.dto;

import com.finediningtheater.inquiry.Proposal;
import java.time.Instant;

public record ProposalAdminResponse(
        Long id,
        String name,
        String contactEmail,
        String category,
        String title,
        String body,
        String status,
        Instant createdAt) {

    public static ProposalAdminResponse from(Proposal proposal) {
        return new ProposalAdminResponse(
                proposal.getId(),
                proposal.getName(),
                proposal.getContactEmail(),
                proposal.getCategory() == null ? null : proposal.getCategory().name(),
                proposal.getTitle(),
                proposal.getBody(),
                proposal.getStatus().name(),
                proposal.getCreatedAt());
    }
}
