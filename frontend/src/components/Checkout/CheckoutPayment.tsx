
import type { Dispatch, SetStateAction } from "react";
import {
    AlertCircleIcon,
    ChevronRightIcon,
    CreditCardIcon,
    WalletCardsIcon,
} from "lucide-react";

interface CheckoutPaymentProps {
    setStep: Dispatch<SetStateAction<string>>;
    paymentMethod: string;
    setPaymentMethod: Dispatch<SetStateAction<string>>;
    fundsError: string | null;
    onAddFunds: () => void;
}

export default function CheckoutPayment({
                                            setStep,
                                            paymentMethod,
                                            setPaymentMethod,
                                            fundsError,
                                            onAddFunds,
                                        }: CheckoutPaymentProps)  {
  return (
    <div className="bg-white rounded-2xl p-6 animate-fade-in">
      <h2 className="text-lg font-semibold text-app-green mb-5 flex items-center gap-2">
        <CreditCardIcon className="size-5" /> Payment Method
      </h2>
      <div className="space-y-3">
        {[
            {
                value: "FUNDS",
                label: "Pay from Funds",
                desc: "Use your available wallet balance",
            },
            {
                value: "CARD",
                label: "Credit / Debit Card",
                desc: "Pay securely with your card",
            },
            {
                value: "CASH_ON_DELIVERY",
                label: "Cash on Delivery",
                desc: "Pay when you receive your order",
            },
        ].map((method) => (
          <label
            key={method.value}
            className={`flex items-center gap-4 p-4 rounded-xl border cursor-pointer transition-all ${paymentMethod === method.value ? "border-app-green bg-app-cream" : "border-app-border hover:border-app-green-lighter"}`}
          >
            <input
              type="radio"
              name="payment"
              value={method.value}
              checked={paymentMethod === method.value}
              onChange={(e) => setPaymentMethod(e.target.value)}
              className="size-4 text-app-green"
            />
            <div>
              <p className="text-sm font-semibold text-app-green">
                {method.label}
              </p>
              <p className="text-xs text-app-text-light">{method.desc}</p>
            </div>
          </label>
        ))}
      </div>
        {paymentMethod === "FUNDS" && fundsError && (
            <div
                role="alert"
                className="mt-5 rounded-xl border border-orange-200 bg-orange-50 p-4"
            >
                <div className="flex gap-3">
                    <AlertCircleIcon className="mt-0.5 size-5 shrink-0 text-app-orange" />

                    <div>
                        <h3 className="font-semibold text-app-green">
                            Your funds are insufficient
                        </h3>

                        <p className="mt-1 text-sm text-app-text-light">
                            {fundsError}
                        </p>

                        <p className="mt-2 text-sm text-app-text-light">
                            Add money to your funds wallet, then return to checkout to place
                            this order.
                        </p>

                        <button
                            type="button"
                            onClick={onAddFunds}
                            className="mt-4 inline-flex items-center gap-2 rounded-xl bg-app-orange px-4 py-2.5 text-sm font-semibold text-white transition-colors hover:bg-orange-600"
                        >
                            <WalletCardsIcon className="size-4" />
                            Add Funds
                        </button>
                    </div>
                </div>
            </div>
        )}
      <button
        onClick={() => {
          setStep("review");
          scrollTo(0, 0);
        }}
        className="mt-6 px-6 py-3 bg-app-green text-white font-semibold rounded-xl hover:bg-app-green-light transition-colors flex items-center gap-2"
      >
        Review Order <ChevronRightIcon className="size-4" />
      </button>
    </div>
  );
}
