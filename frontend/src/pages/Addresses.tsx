import { useEffect, useState } from "react";
import toast from "react-hot-toast";
import { MapPinIcon, PencilIcon } from "lucide-react";

import Loading from "../components/Loading";
import { useAuth } from "../context/AuthContext";
import api from "../config/api";

const Addresses = () => {
  const { user, updateUser } = useAuth();

  const [address, setAddress] = useState("");
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [editing, setEditing] = useState(false);

  useEffect(() => {
    if (!user) {
      setLoading(false);
      return;
    }

    api
        .get(`/users/${user.id}`)
        .then(({ data }) => {
          setAddress(data.address || "");
          updateUser({ address: data.address || "" });
        })
        .catch((error) => {
          toast.error(
              error.response?.data?.message || "Could not load address",
          );
        })
        .finally(() => setLoading(false));
  }, [user?.id]);

  const saveAddress = async () => {
    if (!user) return;

    if (!address.trim()) {
      toast.error("Please enter a delivery address.");
      return;
    }

    setSaving(true);

    try {
      const { data } = await api.patch(`/users/${user.id}`, {
        address: address.trim(),
      });

      setAddress(data.address);
      updateUser({ address: data.address });
      setEditing(false);

      toast.success("Delivery address updated.");
    } catch (error: any) {
      toast.error(
          error.response?.data?.message || "Could not update address.",
      );
    } finally {
      setSaving(false);
    }
  };

  if (loading) return <Loading />;

  return (
      <div className="min-h-screen bg-app-cream">
        <div className="max-w-3xl mx-auto px-4 py-8">
          <h1 className="text-2xl font-semibold text-app-green mb-6">
            Delivery Address
          </h1>

          <div className="bg-white border border-app-border rounded-2xl p-6">
            <div className="flex items-start gap-3">
              <MapPinIcon className="size-5 text-app-green mt-1" />

              <div className="flex-1">
                <h2 className="font-semibold text-app-green">
                  Your saved address
                </h2>

                {editing ? (
                    <>
                  <textarea
                      value={address}
                      onChange={(event) => setAddress(event.target.value)}
                      rows={4}
                      placeholder="Enter your complete delivery address"
                      className="w-full mt-4 p-3 text-sm border border-app-border rounded-xl outline-none focus:border-app-green"
                  />

                      <div className="flex gap-3 mt-4">
                        <button
                            onClick={saveAddress}
                            disabled={saving}
                            className="px-5 py-2.5 bg-app-green text-white rounded-xl disabled:opacity-60"
                        >
                          {saving ? "Saving..." : "Save Address"}
                        </button>

                        <button
                            onClick={() => setEditing(false)}
                            className="px-5 py-2.5 border border-app-border rounded-xl"
                        >
                          Cancel
                        </button>
                      </div>
                    </>
                ) : (
                    <>
                      <p className="text-sm text-app-text-light mt-2 whitespace-pre-line">
                        {address || "No delivery address saved yet."}
                      </p>

                      <button
                          onClick={() => setEditing(true)}
                          className="mt-4 flex items-center gap-2 px-4 py-2 text-sm bg-app-cream text-app-green rounded-xl"
                      >
                        <PencilIcon className="size-4" />
                        {address ? "Update Address" : "Add Address"}
                      </button>
                    </>
                )}
              </div>
            </div>
          </div>
        </div>
      </div>
  );
};

export default Addresses;