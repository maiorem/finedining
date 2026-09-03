import { apiAdminGet } from "./adminHttp";
import type { ProposalCategory } from "./proposals";

export type ProposalStatus = "SENT" | "READ" | "ACCEPTED" | "DECLINED";

export type ProposalAdmin = {
  id: number;
  name: string;
  contactEmail: string;
  category: ProposalCategory | null;
  title: string;
  body: string;
  status: ProposalStatus;
  createdAt: string;
};

export function listProposalsForAdmin(accessToken: string): Promise<ProposalAdmin[]> {
  return apiAdminGet<ProposalAdmin[]>("/api/proposals/manage", accessToken);
}
