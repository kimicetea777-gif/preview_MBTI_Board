import { useState } from "react";

export function usePhoneInput() {
  const [phone, setPhone] = useState("");

  const handlePhoneChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const val = e.target.value.replace(/[^0-9]/g, "");
    if (val.length <= 11) {
      setPhone(val.replace(/^(\d{2,3})(\d{3,4})(\d{4})$/, "$1-$2-$3"));
    }
  };

  return { phone, setPhone, handlePhoneChange };
}
