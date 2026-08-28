import { apiAdminGet } from "./adminHttp";

export type ProposalStatus = "SENT" | "READ" | "ACCEPTED" | "DECLINED";

export type ProposalAdmin = {
  id: number;
  name: string;
  contactEmail: string;
  title: string;
  body: string;
  status: ProposalStatus;
  createdAt: string;
};

export function listProposalsForAdmin(accessToken: string): Promise<ProposalAdmin[]> {
  return apiAdminGet<ProposalAdmin[]>("/api/proposals/manage", accessToken);
}
