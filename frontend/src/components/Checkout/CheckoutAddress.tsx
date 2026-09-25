import { ChevronRightIcon, MapPinIcon } from "lucide-react";

type CheckoutAddressProps = {
  deliveryAddress: string;
  setDeliveryAddress: (address: string) => void;
  profileAddress?: string;
  previousAddresses: string[];
  setStep: (step: string) => void;
};

const CheckoutAddress = ({ deliveryAddress, setDeliveryAddress, profileAddress, previousAddresses, setStep }: CheckoutAddressProps) => {
  const choices = [...new Set([profileAddress, ...previousAddresses].filter(Boolean) as string[])];

  return (
    <div className="bg-white rounded-2xl p-6 animate-fade-in">
      <h2 className="text-lg font-semibold text-app-green mb-5 flex items-center gap-2">
        <MapPinIcon className="size-5" /> Delivery Address
      </h2>
      {choices.length > 0 && (
        <div className="mb-6">
          <h3 className="text-sm font-semibold text-app-green mb-3">
            Choose an address
          </h3>
          <div className="space-y-3">
            {choices.map((choice, index) => (
              <button
                key={choice}
                type="button"
                onClick={() => setDeliveryAddress(choice)}
                className={`w-full rounded-xl border p-4 text-left transition-colors ${deliveryAddress === choice ? "border-app-green bg-app-cream" : "border-app-border hover:bg-app-cream"}`}
              >
                <div className="flex items-center gap-2 mb-1">
                  <MapPinIcon className="size-4 text-app-green" />
                  <span className="font-semibold text-zinc-900 text-sm">
                    {index === 0 && profileAddress === choice ? "Profile address" : "Previously used for an order"}
                  </span>
                </div>
                <p className="text-sm text-zinc-600">{choice}</p>
              </button>
            ))}
          </div>
        </div>
      )}
      <label className="block text-sm font-semibold text-app-green">
        Delivery address for this order
        <textarea
          value={deliveryAddress}
          onChange={(event) => setDeliveryAddress(event.target.value)}
          rows={4}
          placeholder="Enter a different delivery address"
          className="mt-2 w-full rounded-xl border border-app-border p-3 text-sm font-normal text-app-text outline-none focus:border-app-green"
        />
      </label>
      <p className="mt-2 text-xs text-app-text-light">This address applies only to this order. It will not update your profile address.</p>
      <button
        onClick={() => {
          setStep("payment");
          scrollTo(0, 0);
        }}
        disabled={!deliveryAddress.trim()}
        className="mt-6 px-6 py-3 bg-app-green text-white font-semibold rounded-xl hover:bg-app-green-light transition-colors disabled:opacity-50 flex items-center gap-2"
      >
        Continue to Payment <ChevronRightIcon className="size-4" />
      </button>
    </div>
  );
};

export default CheckoutAddress;

