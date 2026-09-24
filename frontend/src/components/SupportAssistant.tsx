import { useEffect, useRef, useState, type FormEvent } from "react";
import { Bot, ExternalLink, Loader2, MessageCircle, Package, Send, ShoppingBag, Sparkles, UserRound, X } from "lucide-react";
import { useLocation, useNavigate } from "react-router-dom";

import { sendSupportMessage, type SupportAssistantResponse } from "../config/SupportAssistantApi";
import { errorMessage } from "../config/api";
import { useAuth } from "../context/AuthContext";

type ConversationMessage = {
  id: string;
  sender: "user" | "assistant";
  text: string;
  result?: SupportAssistantResponse;
};

const customerPrompts = ["Find discounted products", "Where is my latest order?", "Show my cart", "How do I change my password?"];
const employeePrompts = ["Show my product requests", "How do I update an order?", "How do account tickets work?", "Open my profile"];
const adminPrompts = ["Show the store overview", "Show pending product requests", "What products are low in stock?", "How do reports work?"];

export default function SupportAssistant() {
  const { user } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);
  const [message, setMessage] = useState("");
  const [working, setWorking] = useState(false);
  const [conversationId, setConversationId] = useState<string>();
  const [messages, setMessages] = useState<ConversationMessage[]>([]);
  const endRef = useRef<HTMLDivElement>(null);
  const userId = user?.id;

  const role = user?.role === "CUSTOMER" ? "customer" : user?.role === "EMPLOYEE" ? "employee" : "admin";
  const prompts = role === "customer" ? customerPrompts : role === "employee" ? employeePrompts : adminPrompts;
  const hidden = !user || location.pathname === "/recipe-assistant" || location.pathname.startsWith("/auth")
    || location.pathname.startsWith("/recover-account") || location.pathname.startsWith("/unlock-account");

  useEffect(() => {
    if (!userId) return;
    const key = `grocers_support_conversation_${userId}`;
    setConversationId(sessionStorage.getItem(key) || undefined);
    setMessages([]);
  }, [userId]);

  useEffect(() => { if (open) endRef.current?.scrollIntoView({ behavior: "smooth" }); }, [messages, open, working]);
  useEffect(() => { setOpen(false); }, [location.pathname]);

  if (hidden) return null;

  const ask = async (text: string) => {
    const question = text.trim();
    if (!question || working) return;
    setMessages(current => [...current, { id: crypto.randomUUID(), sender: "user", text: question }]);
    setMessage("");
    setWorking(true);
    try {
      const result = await sendSupportMessage(question, conversationId);
      setConversationId(result.conversationId);
      sessionStorage.setItem(`grocers_support_conversation_${user.id}`, result.conversationId);
      setMessages(current => [...current, { id: crypto.randomUUID(), sender: "assistant", text: result.reply, result }]);
    } catch (error) {
      setMessages(current => [...current, { id: crypto.randomUUID(), sender: "assistant", text: errorMessage(error, "I could not reach Grocers Help right now. Please try again shortly.") }]);
    } finally {
      setWorking(false);
    }
  };

  const submit = (event: FormEvent) => { event.preventDefault(); void ask(message); };
  const go = (path?: string) => { if (!path) return; setOpen(false); navigate(path); };

  return <>
    <button type="button" onClick={() => setOpen(value => !value)} aria-label={open ? "Close Grocers Help" : "Open Grocers Help"}
      className="fixed bottom-5 right-5 z-[80] flex h-14 items-center gap-2 rounded-full bg-[#173b28] px-4 text-sm font-bold text-white shadow-2xl shadow-emerald-950/30 transition hover:-translate-y-0.5 hover:bg-[#215238]">
      {open ? <X size={21}/> : <MessageCircle size={21}/>}<span className="hidden sm:inline">Ask Grocers</span>
    </button>

    {open && <section aria-label="Grocers Help Assistant" className="fixed bottom-24 right-4 z-[79] flex h-[min(720px,calc(100vh-7rem))] w-[min(420px,calc(100vw-2rem))] flex-col overflow-hidden rounded-3xl border border-emerald-950/10 bg-[#fbfcfa] shadow-[0_30px_90px_rgba(16,48,31,.28)]">
      <header className="bg-[#173b28] px-5 py-4 text-white">
        <div className="flex items-center gap-3"><span className="grid size-10 place-items-center rounded-2xl bg-white/10 text-orange-300"><Sparkles size={20}/></span><div className="min-w-0 flex-1"><h2 className="font-bold">Grocers Help</h2><p className="truncate text-xs text-white/60">Platform support · live account information</p></div><button onClick={() => setOpen(false)} className="grid size-9 place-items-center rounded-xl text-white/70 hover:bg-white/10 hover:text-white" aria-label="Close assistant"><X size={18}/></button></div>
      </header>

      <div className="flex-1 overflow-y-auto px-4 py-5">
        {!messages.length && <div>
          <div className="flex gap-3"><span className="grid size-9 shrink-0 place-items-center rounded-xl bg-emerald-100 text-[#24583b]"><Bot size={18}/></span><div className="rounded-2xl rounded-tl-md border border-[#e1e9e2] bg-white p-4 text-sm leading-6 text-[#425248] shadow-sm"><b className="block text-[#173b28]">Hi {user.firstName || "there"}.</b>I can help with Grocers features and retrieve live information allowed for your {role} account. The Recipe Planner remains a separate feature.</div></div>
          <p className="mb-2 mt-5 text-[10px] font-bold uppercase tracking-[0.16em] text-[#89968d]">Try asking</p>
          <div className="grid gap-2">{prompts.map(prompt => <button key={prompt} onClick={() => void ask(prompt)} className="rounded-xl border border-[#dfe7e0] bg-white px-3 py-2.5 text-left text-sm font-semibold text-[#2f4938] transition hover:border-emerald-300 hover:bg-emerald-50">{prompt}</button>)}</div>
          {role === "customer" && <button onClick={() => go("/recipe-assistant")} className="mt-3 flex w-full items-center justify-between rounded-xl bg-orange-50 px-3 py-3 text-left text-sm font-semibold text-orange-700"><span>Want meal recommendations? Open Recipe Planner</span><ExternalLink size={15}/></button>}
        </div>}

        <div className="space-y-4">{messages.map(item => item.sender === "user"
          ? <div key={item.id} className="ml-auto max-w-[84%] rounded-2xl rounded-br-md bg-[#173b28] px-4 py-3 text-sm leading-6 text-white">{item.text}</div>
          : <div key={item.id} className="flex gap-2.5"><span className="grid size-8 shrink-0 place-items-center rounded-xl bg-emerald-100 text-[#24583b]"><Bot size={16}/></span><div className="min-w-0 flex-1"><div className="rounded-2xl rounded-tl-md border border-[#e1e9e2] bg-white px-4 py-3 text-sm leading-6 text-[#425248] shadow-sm whitespace-pre-wrap">{item.text}</div>{item.result && <AssistantResult result={item.result} go={go}/>}</div></div>)}
          {working && <div className="flex items-center gap-2.5 text-sm text-[#718077]"><span className="grid size-8 place-items-center rounded-xl bg-emerald-100 text-[#24583b]"><Bot size={16}/></span><span className="flex items-center gap-2 rounded-2xl border border-[#e1e9e2] bg-white px-4 py-3"><Loader2 className="animate-spin" size={15}/>Checking Grocers…</span></div>}
        </div><div ref={endRef}/>
      </div>

      <form onSubmit={submit} className="border-t border-[#e1e8e2] bg-white p-3"><div className="flex items-end gap-2 rounded-2xl border border-[#dce5de] bg-[#f8faf8] p-2 focus-within:border-emerald-400 focus-within:ring-2 focus-within:ring-emerald-100"><textarea rows={1} maxLength={1000} value={message} onChange={event => setMessage(event.target.value)} onKeyDown={event => { if (event.key === "Enter" && !event.shiftKey) { event.preventDefault(); if (message.trim()) void ask(message); } }} placeholder="Ask about Grocers…" className="max-h-28 min-h-10 flex-1 resize-none bg-transparent px-2 py-2 text-sm text-[#24362a] outline-none"/><button disabled={working || !message.trim()} className="grid size-10 shrink-0 place-items-center rounded-xl bg-[#ef7622] text-white transition hover:bg-[#da6417] disabled:cursor-not-allowed disabled:opacity-40" aria-label="Send message"><Send size={17}/></button></div><p className="mt-2 text-center text-[10px] text-[#8a968e]">Grocers Help can make mistakes. Confirm important details on the linked page.</p></form>
    </section>}
  </>;
}

function AssistantResult({ result, go }: { result: SupportAssistantResponse; go: (path?: string) => void }) {
  return <div className="mt-2 space-y-2">
    {result.cards.map((card, index) => <article key={`${card.type}-${card.title}-${index}`} className="overflow-hidden rounded-2xl border border-[#dfe7e0] bg-white shadow-sm">
      <div className="flex gap-3 p-3">{card.imageUrl ? <img src={card.imageUrl} alt="" className="size-16 shrink-0 rounded-xl bg-[#f3f5f2] object-cover"/> : <span className="grid size-12 shrink-0 place-items-center rounded-xl bg-[#eef4ef] text-[#35704c]">{card.type === "ORDER" ? <ShoppingBag size={19}/> : card.type === "ACCOUNT" ? <UserRound size={19}/> : <Package size={19}/>}</span>}<div className="min-w-0 flex-1"><b className="block truncate text-sm text-[#1c3023]">{card.title || "Grocers information"}</b>{card.subtitle && <p className="mt-0.5 line-clamp-2 text-xs leading-5 text-[#7b887f]">{card.subtitle}</p>}<dl className="mt-2 grid grid-cols-2 gap-x-3 gap-y-1">{Object.entries(card.details || {}).slice(0, 6).map(([label, value]) => <div key={label} className="min-w-0"><dt className="truncate text-[9px] font-bold uppercase tracking-wider text-[#9aa49d]">{label}</dt><dd className="truncate text-xs font-semibold text-[#3c5043]">{value || "—"}</dd></div>)}</dl></div></div>{card.link && <button onClick={() => go(card.link)} className="flex w-full items-center justify-between border-t border-[#edf1ed] px-3 py-2.5 text-xs font-bold text-[#28623f] hover:bg-emerald-50"><span>{card.linkLabel || "Open"}</span><ExternalLink size={13}/></button>}
    </article>)}
    {!!result.actions.length && <div className="flex flex-wrap gap-2">{result.actions.map(action => <button key={`${action.link}-${action.label}`} onClick={() => go(action.link)} className={action.style === "primary" ? "rounded-xl bg-[#24583b] px-3 py-2 text-xs font-bold text-white" : "rounded-xl border border-[#dce5de] bg-white px-3 py-2 text-xs font-bold text-[#365240]"}>{action.label}</button>)}</div>}
    {!!result.sources.length && <p className="px-1 text-[9px] font-semibold uppercase tracking-wider text-[#9aa49d]">Source: {result.sources.join(" · ")}</p>}
  </div>;
}
