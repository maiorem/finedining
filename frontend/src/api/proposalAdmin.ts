import { apiAdminGet, apiAdminPost } from "./adminHttp";

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

/** 상세 조회 자체가 "읽음" 처리다 — SENT였다면 READ로 바뀐다(CLAUDE.md §3.7). */
export function getProposalForAdmin(accessToken: string, id: number): Promise<ProposalAdmin> {
  return apiAdminGet<ProposalAdmin>(`/api/proposals/manage/${id}`, accessToken);
}

export function acceptProposal(accessToken: string, id: number): Promise<ProposalAdmin> {
  return apiAdminPost<ProposalAdmin>(`/api/proposals/${id}/accept`, accessToken);
}

export function declineProposal(accessToken: string, id: number): Promise<ProposalAdmin> {
  return apiAdminPost<ProposalAdmin>(`/api/proposals/${id}/decline`, accessToken);
}
