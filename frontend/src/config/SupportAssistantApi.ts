import api from "./api";

export type SupportAssistantCard = {
  type: "PRODUCT" | "ORDER" | "CART" | "REQUEST" | "REPORT" | "ACCOUNT" | string;
  title: string;
  subtitle?: string;
  imageUrl?: string;
  details: Record<string, string>;
  link?: string;
  linkLabel?: string;
};

export type SupportAssistantAction = { label: string; link: string; style: "primary" | "secondary" | string };

export type SupportAssistantResponse = {
  conversationId: string;
  reply: string;
  cards: SupportAssistantCard[];
  actions: SupportAssistantAction[];
  sources: string[];
};

export async function sendSupportMessage(message: string, conversationId?: string) {
  // Send the conversation ID back so the service can keep the chat context.
  const { data } = await api.post<SupportAssistantResponse>("/support-assistant/chat", { message, conversationId });
  return data;
}
