"use client";

import Image from "next/image";
import { useEffect, useMemo, useState } from "react";

type IconName =
  | "games"
  | "cart"
  | "card"
  | "wallet"
  | "play"
  | "spark"
  | "qr"
  | "atm"
  | "ewallet"
  | "nfc";

const currencyFormatter = new Intl.NumberFormat("vi-VN", {
  style: "currency",
  currency: "VND",
  maximumFractionDigits: 0,
});

const quickActions = [
  {
    id: "games",
    label: "Xem trò chơi",
    description: "Khám phá 20+ trò hot",
    icon: "games",
  },
  {
    id: "purchase",
    label: "Mua trò chơi",
    description: "Combo tiết kiệm",
    icon: "cart",
  },
  {
    id: "topup",
    label: "Nạp tiền",
    description: "QR, ATM, ví",
    icon: "wallet",
  },
  {
    id: "card",
    label: "Kiểm tra thẻ",
    description: "Số dư & gói",
    icon: "card",
  },
  {
    id: "play",
    label: "Chơi trò chơi",
    description: "Quét thẻ & bắt đầu",
    icon: "play",
  },
] as const;

const gameFilters = [
  { label: "Mọi lứa tuổi", value: "all" },
  { label: "3-6 tuổi", value: "kid" },
  { label: "Gia đình", value: "family" },
  { label: "Thể thao", value: "sport" },
  { label: "Giá < 50k", value: "budget" },
] as const;

const games = [
  {
    id: "sky-blaster",
    name: "Sky Blaster VR",
    description: "Bay lượn trong không gian ảo với ghế chuyển động 360°.",
    price: 65000,
    tags: ["Mạo hiểm", "8+"],
    ageGroup: "family",
    category: "adventure",
    image:
      "https://images.unsplash.com/photo-1506744038136-46273834b3fb?auto=format&fit=crop&w=1200&q=80",
  },
  {
    id: "bubble-race",
    name: "Bubble Race",
    description: "Đua xe bong bóng mềm dành cho bé từ 3 tuổi.",
    price: 45000,
    tags: ["Nhí", "3-6 tuổi"],
    ageGroup: "kid",
    category: "family",
    image:
      "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?auto=format&fit=crop&w=1200&q=80",
  },
  {
    id: "dance-fam",
    name: "Dance Party Fam",
    description: "Lắc lư theo nhạc với bảng LED cảm ứng toàn thân.",
    price: 55000,
    tags: ["Gia đình", "Âm nhạc"],
    ageGroup: "family",
    category: "sport",
    image:
      "https://images.unsplash.com/photo-1504609813442-a8924e83f76e?auto=format&fit=crop&w=1200&q=80",
  },
  {
    id: "neo-hoop",
    name: "Neo Hoop Arena",
    description: "Ném bóng rổ ánh sáng với cảm biến điểm số thời gian thực.",
    price: 50000,
    tags: ["Thể thao", "Thi đấu"],
    ageGroup: "family",
    category: "sport",
    image:
      "https://images.unsplash.com/photo-1519861155734-0a61bcc6d140?auto=format&fit=crop&w=1200&q=80",
  },
] as const;

const playPackages = [
  {
    id: "single",
    name: "1 lần",
    turns: 1,
    price: 45000,
    highlight: "Thử nhanh mọi trò",
    benefit: "Không giới hạn loại trò",
  },
  {
    id: "combo3",
    name: "Combo 3 lần",
    turns: 3,
    price: 120000,
    highlight: "Tiết kiệm 15%",
    benefit: "Tặng sticker linh vật",
  },
  {
    id: "combo5",
    name: "Combo 5 lần",
    turns: 5,
    price: 180000,
    highlight: "Tiết kiệm 25%",
    benefit: "Bonus 1 lần minigame",
  },
] as const;

const topUpOptions = [
  {
    id: "50k",
    amount: 50000,
    bonus: "Tặng 1 lượt minigame",
    recommended: false,
  },
  {
    id: "100k",
    amount: 100000,
    bonus: "Tặng 1 lượt quay thưởng",
    recommended: true,
  },
  {
    id: "200k",
    amount: 200000,
    bonus: "Tặng 1 ảnh 3D gia đình",
    recommended: false,
  },
  {
    id: "500k",
    amount: 500000,
    bonus: "Tặng 2 combo 3 lần",
    recommended: false,
  },
] as const;

const paymentMethods = [
  {
    id: "qr",
    title: "QR Pay",
    description: "Quét mọi app ngân hàng",
    icon: "qr",
  },
  {
    id: "atm",
    title: "Thẻ ATM",
    description: "Hỗ trợ 45+ ngân hàng",
    icon: "atm",
  },
  {
    id: "ewallet",
    title: "Ví điện tử",
    description: "Momo, ZaloPay, VNPay",
    icon: "ewallet",
  },
] as const;

const cardStatus = {
  balance: 350000,
  remainingPlays: 8,
  packages: ["Combo 5 lần", "VR Day Pass"],
  expireAt: "31/12/2025",
};

const playStatuses = [
  { id: "enough", label: "Đủ tiền", level: "success" },
  { id: "ready", label: "Sẵn sàng chơi", level: "info" },
  { id: "charging", label: "Đã trừ tiền", level: "done" },
] as const;

const formatCurrency = (value: number) => currencyFormatter.format(value);

export default function Home() {
  const [activeFilter, setActiveFilter] = useState<
    (typeof gameFilters)[number]["value"]
  >(gameFilters[0].value);
  const [selectedPackageId, setSelectedPackageId] = useState<
    (typeof playPackages)[number]["id"]
  >(playPackages[1].id);
  const [selectedTopUpId, setSelectedTopUpId] = useState<
    (typeof topUpOptions)[number]["id"]
  >(topUpOptions[1].id);
  const [cardCode, setCardCode] = useState("JOY-2846-TH");

  const selectedPackage = useMemo(
    () =>
      playPackages.find((pkg) => pkg.id === selectedPackageId) ??
      playPackages[0],
    [selectedPackageId]
  );

  const selectedTopUp = useMemo(
    () =>
      topUpOptions.find((pkg) => pkg.id === selectedTopUpId) ?? topUpOptions[0],
    [selectedTopUpId]
  );

  const filteredGames = useMemo(() => {
    return games.filter((game) => {
      if (activeFilter === "all") return true;
      if (activeFilter === "kid") return game.ageGroup === "kid";
      if (activeFilter === "family") return game.ageGroup === "family";
      if (activeFilter === "sport") return game.category === "sport";
      if (activeFilter === "budget") return game.price <= 50000;
      return true;
    });
  }, [activeFilter]);

  const handleScroll = (sectionId: string) => {
    const element = document.getElementById(sectionId);
    element?.scrollIntoView({ behavior: "smooth", block: "start" });
  };

  return (
    <div className="min-h-screen bg-linear-to-br from-sky-50 via-white to-orange-50 text-slate-900">
      <div className="mx-auto flex w-full max-w-6xl flex-col gap-8 px-6 py-10 pb-20">
        <header className="sticky top-4 z-10 rounded-3xl bg-white/80 px-6 py-4 backdrop-blur-lg shadow-lg shadow-sky-100">
          <div className="flex flex-wrap items-center justify-between gap-4">
            <div>
              <p className="text-xs font-semibold uppercase tracking-[0.2em] text-sky-600">
                JoyZone Self-Service
              </p>
              <h1 className="text-2xl font-black text-slate-900">
                Smart Card Play Hub
              </h1>
            </div>
            <div className="flex flex-wrap gap-2 text-sm font-medium text-slate-500">
              <span className="rounded-full bg-sky-50 px-4 py-2 text-sky-600">
                100% tự thao tác
              </span>
              <span className="rounded-full bg-orange-50 px-4 py-2 text-orange-600">
                Màn hình cảm ứng 24&quot;
              </span>
            </div>
          </div>
        </header>

        {/* Home Screen */}
        <section
          id="home"
          className="grid gap-6 rounded-4xl bg-white/90 p-8 shadow-xl shadow-sky-100/70 ring-1 ring-white/70 lg:grid-cols-[1.1fr,0.9fr]"
        >
          <div className="flex flex-col gap-6">
            <div>
              <p className="text-sm font-semibold uppercase tracking-[0.3em] text-sky-500">
                Chào mừng
              </p>
              <h2 className="mt-2 text-4xl font-black text-slate-900">
                Chạm để bắt đầu chuyến phiêu lưu trò chơi
              </h2>
              <p className="mt-3 text-lg text-slate-600">
                Kiosk tự phục vụ dành cho trẻ em & gia đình. Nút lớn, icon rõ
                ràng, phù hợp mọi độ tuổi.
              </p>
            </div>
            <div className="grid gap-4 md:grid-cols-2">
              {quickActions.map((action) => (
                <button
                  key={action.id}
                  onClick={() => handleScroll(action.id)}
                  className="flex flex-col gap-3 rounded-3xl border border-transparent bg-linear-to-br from-sky-50 to-white p-5 text-left shadow-[0_12px_30px_rgba(15,118,110,0.12)] transition hover:-translate-y-1 hover:border-sky-200 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-sky-500"
                >
                  <div className="flex items-center gap-3 text-sky-600">
                    <IconBadge name={action.icon} />
                    <div>
                      <p className="text-lg font-semibold text-slate-900">
                        {action.label}
                      </p>
                      <p className="text-sm text-slate-500">
                        {action.description}
                      </p>
                    </div>
                  </div>
                  <span className="text-sm font-semibold text-orange-500">
                    Chạm để mở →
                  </span>
                </button>
              ))}
            </div>
          </div>
          <div className="relative overflow-hidden rounded-4xl bg-linear-to-br from-sky-500 via-blue-600 to-indigo-700 p-8 text-white">
            <div className="flex flex-col gap-4">
              <p className="text-sm font-semibold uppercase tracking-[0.4em] text-white/80">
                Linh vật
              </p>
              <h3 className="text-3xl font-black">JoyBot</h3>
              <p className="text-white/80">
                Linh vật 3D dẫn đường, hướng dẫn từng bước trên màn hình cảm ứng
                tự phục vụ.
              </p>
            </div>
            <div className="mt-6 flex items-center justify-center">
              <Image
                src="/mascot.svg"
                width={260}
                height={260}
                alt="JoyBot mascot"
                priority
              />
            </div>
            <div className="mt-6 flex flex-wrap gap-3 text-sm">
              <span className="rounded-full bg-white/15 px-4 py-2">
                Cảm biến NFC
              </span>
              <span className="rounded-full bg-white/15 px-4 py-2">
                Âm thanh hướng dẫn
              </span>
              <span className="rounded-full bg-white/15 px-4 py-2">
                Hỗ trợ 3 ngôn ngữ
              </span>
            </div>
          </div>
        </section>

        {/* Game Browser */}
        <Section
          id="games"
          badge="Xem trò chơi"
          title="Danh sách trò chơi nổi bật"
          subtitle="Chạm để lọc nhanh theo độ tuổi, thể loại hoặc ngân sách."
        >
          <div className="flex flex-wrap gap-3">
            {gameFilters.map((filter) => (
              <button
                key={filter.value}
                onClick={() => setActiveFilter(filter.value)}
                className={`rounded-full px-5 py-2 text-sm font-semibold transition ${
                  activeFilter === filter.value
                    ? "bg-linear-to-r from-sky-500 to-blue-600 text-white shadow-lg shadow-sky-200"
                    : "bg-slate-100 text-slate-500 hover:bg-white"
                }`}
              >
                {filter.label}
              </button>
            ))}
          </div>

          <div className="grid gap-6 md:grid-cols-2">
            {filteredGames.map((game) => (
              <article
                key={game.id}
                className="overflow-hidden rounded-3xl bg-white shadow-[0_20px_45px_rgba(15,23,42,0.08)]"
              >
                <div
                  className="h-48 w-full bg-cover bg-center"
                  style={{ backgroundImage: `url(${game.image})` }}
                />
                <div className="space-y-4 p-6">
                  <div className="flex items-start justify-between gap-3">
                    <div>
                      <h3 className="text-xl font-bold text-slate-900">
                        {game.name}
                      </h3>
                      <p className="text-sm text-slate-500">
                        {game.description}
                      </p>
                    </div>
                    <span className="rounded-2xl bg-orange-50 px-3 py-1 text-sm font-black text-orange-500">
                      {formatCurrency(game.price)}
                    </span>
                  </div>
                  <div className="flex flex-wrap gap-2 text-xs font-semibold text-slate-500">
                    {game.tags.map((tag) => (
                      <span
                        key={tag}
                        className="rounded-full bg-slate-100 px-3 py-1"
                      >
                        {tag}
                      </span>
                    ))}
                  </div>
                  <button className="w-full rounded-2xl bg-linear-to-r from-orange-400 to-pink-500 py-3 text-center text-base font-bold text-white shadow-lg shadow-orange-200 transition hover:-translate-y-0.5">
                    Chơi ngay
                  </button>
                </div>
              </article>
            ))}
          </div>
        </Section>

        {/* Purchase */}
        <Section
          id="purchase"
          badge="Mua trò chơi"
          title="Chọn gói chơi phù hợp"
          subtitle="Combo tối ưu cho gia đình – hiển thị tổng tiền và nút thanh toán rõ ràng."
        >
          <div className="grid gap-4 md:grid-cols-3">
            {playPackages.map((pkg) => {
              const isActive = pkg.id === selectedPackageId;
              return (
                <button
                  key={pkg.id}
                  onClick={() => setSelectedPackageId(pkg.id)}
                  className={`flex h-full flex-col rounded-3xl border-2 p-5 text-left transition ${
                    isActive
                      ? "border-transparent bg-linear-to-br from-sky-500 to-blue-600 text-white shadow-xl shadow-sky-200"
                      : "border-slate-100 bg-white hover:border-sky-200"
                  }`}
                >
                  <p
                    className={`text-sm font-semibold ${
                      isActive ? "text-white/80" : "text-sky-500"
                    }`}
                  >
                    {pkg.highlight}
                  </p>
                  <h3 className="mt-2 text-3xl font-black">{pkg.name}</h3>
                  <p className="text-5xl font-black tracking-tight">
                    {formatCurrency(pkg.price)}
                  </p>
                  <p
                    className={`mt-3 text-sm ${
                      isActive ? "text-white/90" : "text-slate-500"
                    }`}
                  >
                    {pkg.benefit}
                  </p>
                  <div
                    className={`mt-auto rounded-2xl px-4 py-3 text-sm font-semibold ${
                      isActive ? "bg-white/20" : "bg-slate-100 text-slate-700"
                    }`}
                  >
                    {pkg.turns} lượt chơi
                  </div>
                </button>
              );
            })}
          </div>
          <div className="flex flex-wrap items-center justify-between gap-4 rounded-3xl bg-slate-900 px-6 py-5 text-white">
            <div>
              <p className="text-sm text-white/70">Tổng tiền</p>
              <p className="text-3xl font-black">
                {formatCurrency(selectedPackage.price)}
              </p>
            </div>
            <button className="flex-1 rounded-2xl bg-linear-to-r from-orange-400 to-pink-500 py-4 text-center text-lg font-bold shadow-xl shadow-orange-300/50 sm:flex-none sm:px-12">
              Thanh toán ngay
            </button>
          </div>
        </Section>

        {/* Top up */}
        <Section
          id="topup"
          badge="Nạp tiền"
          title="Nạp tiền cực nhanh"
          subtitle="Theo dõi số dư hiện tại và chọn gói nạp mong muốn."
        >
          <div className="grid gap-6 lg:grid-cols-[1fr,1fr]">
            <div className="space-y-4 rounded-3xl bg-slate-900 p-6 text-white">
              <p className="text-sm text-white/70">Số dư hiện tại</p>
              <p className="text-4xl font-black">
                {formatCurrency(cardStatus.balance)}
              </p>
              <div className="grid grid-cols-2 gap-3 text-sm font-semibold text-white/80">
                <div className="rounded-2xl bg-white/10 px-4 py-3">
                  <p className="text-xs uppercase text-white/50">
                    Lượt chơi còn lại
                  </p>
                  <p className="text-2xl font-black">
                    {cardStatus.remainingPlays}
                  </p>
                </div>
                <div className="rounded-2xl bg-white/10 px-4 py-3">
                  <p className="text-xs uppercase text-white/50">Hết hạn</p>
                  <p className="text-xl font-bold">{cardStatus.expireAt}</p>
                </div>
              </div>
            </div>
            <div className="grid gap-4">
              <div className="grid grid-cols-2 gap-3">
                {topUpOptions.map((option) => {
                  const isActive = option.id === selectedTopUpId;
                  return (
                    <button
                      key={option.id}
                      onClick={() => setSelectedTopUpId(option.id)}
                      className={`flex flex-col rounded-3xl border-2 p-4 text-left transition ${
                        isActive
                          ? "border-transparent bg-linear-to-br from-orange-400 to-pink-500 text-white shadow-lg shadow-orange-200"
                          : "border-slate-100 bg-white hover:border-orange-200"
                      }`}
                    >
                      {option.recommended && (
                        <span
                          className={`mb-2 inline-flex items-center gap-1 rounded-full px-3 py-1 text-xs font-bold ${
                            isActive
                              ? "bg-white/20"
                              : "bg-orange-50 text-orange-500"
                          }`}
                        >
                          <IconSvg name="spark" className="h-3.5 w-3.5" />
                          Phổ biến
                        </span>
                      )}
                      <p className="text-2xl font-black">
                        {formatCurrency(option.amount)}
                      </p>
                      <p
                        className={`text-sm ${
                          isActive ? "text-white/90" : "text-slate-500"
                        }`}
                      >
                        {option.bonus}
                      </p>
                    </button>
                  );
                })}
              </div>
              <div className="flex flex-wrap gap-3">
                {paymentMethods.map((method) => (
                  <div
                    key={method.id}
                    className="flex flex-1 items-center gap-3 rounded-2xl border border-slate-100 bg-white px-4 py-3 text-sm font-semibold text-slate-600 shadow-sm"
                  >
                    <IconBadge name={method.icon} />
                    <div>
                      <p className="text-slate-900">{method.title}</p>
                      <p className="text-xs text-slate-500">
                        {method.description}
                      </p>
                    </div>
                  </div>
                ))}
              </div>
              <div className="flex flex-wrap items-center justify-between gap-3 rounded-2xl bg-slate-100 px-4 py-3 text-sm font-semibold">
                <span>Gói đã chọn</span>
                <span className="text-slate-900">
                  {formatCurrency(selectedTopUp.amount)}
                </span>
              </div>
            </div>
          </div>
        </Section>

        {/* Card status */}
        <Section
          id="card"
          badge="Kiểm tra thẻ"
          title="Thông tin thẻ thông minh"
          subtitle="Theo dõi số dư, gói đã mua và hạn sử dụng theo thời gian thực."
        >
          <div className="grid gap-5 lg:grid-cols-[1.2fr,0.8fr]">
            <div className="rounded-3xl bg-linear-to-br from-sky-500 to-blue-700 p-6 text-white shadow-lg">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm text-white/70">Thẻ JoyZone</p>
                  <p className="text-3xl font-black">
                    {formatCurrency(cardStatus.balance)}
                  </p>
                </div>
                <IconBadge name="card" />
              </div>
              <div className="mt-6 grid gap-4 text-sm font-semibold text-white/80">
                <div className="rounded-2xl bg-white/15 px-4 py-3">
                  <p className="text-xs uppercase text-white/50">
                    Lượt chơi còn
                  </p>
                  <p className="text-2xl font-black text-white">
                    {cardStatus.remainingPlays}
                  </p>
                </div>
                <div className="rounded-2xl bg-white/15 px-4 py-3">
                  <p className="text-xs uppercase text-white/50">Gói đã mua</p>
                  <p>{cardStatus.packages.join(", ")}</p>
                </div>
                <div className="rounded-2xl bg-white/15 px-4 py-3">
                  <p className="text-xs uppercase text-white/50">Thời hạn</p>
                  <p>{cardStatus.expireAt}</p>
                </div>
              </div>
            </div>
            <div className="flex flex-col gap-4">
              <button className="rounded-3xl bg-linear-to-r from-orange-400 to-pink-500 px-4 py-4 text-xl font-bold text-white shadow-lg shadow-orange-200">
                Nạp thêm
              </button>
              <button className="rounded-3xl border-2 border-dashed border-slate-200 px-4 py-4 text-xl font-bold text-slate-600 hover:border-slate-400">
                Mua thêm lượt chơi
              </button>
              <p className="text-sm text-slate-500">
                *Thông tin cập nhật tức thời sau mỗi thao tác trên kiosk.
              </p>
            </div>
          </div>
        </Section>

        {/* Play */}
        <Section
          id="play"
          badge="Chơi trò chơi"
          title="Quét thẻ & bắt đầu chơi"
          subtitle="Hỗ trợ NFC hoặc nhập mã thẻ thủ công. Hiển thị tình trạng tài khoản rõ ràng."
        >
          <div className="grid gap-6 lg:grid-cols-[1fr,0.8fr]">
            <div className="space-y-4 rounded-3xl border border-slate-100 bg-white p-6 shadow-md">
              <div className="rounded-2xl border-2 border-dashed border-sky-200 bg-sky-50/70 p-6 text-center">
                <IconSvg
                  name="nfc"
                  className="mx-auto h-16 w-16 text-sky-600"
                />
                <p className="mt-2 text-lg font-semibold text-slate-900">
                  Chạm thẻ vào vùng NFC
                </p>
                <p className="text-sm text-slate-500">
                  Giữ thẻ trong 2 giây để xác nhận
                </p>
              </div>
              <label className="block text-sm font-semibold text-slate-600">
                Hoặc nhập mã thẻ
                <input
                  className="mt-2 w-full rounded-2xl border border-slate-200 px-4 py-3 text-lg font-bold tracking-widest uppercase shadow-inner focus:border-sky-400 focus:outline-none"
                  value={cardCode}
                  onChange={(event) =>
                    setCardCode(event.target.value.toUpperCase())
                  }
                />
              </label>
              <div className="flex flex-wrap gap-2">
                {playStatuses.map((status) => (
                  <span
                    key={status.id}
                    className={`rounded-full px-4 py-2 text-sm font-bold ${
                      status.level === "success"
                        ? "bg-emerald-100 text-emerald-600"
                        : status.level === "info"
                        ? "bg-sky-100 text-sky-600"
                        : "bg-orange-100 text-orange-600"
                    }`}
                  >
                    {status.label}
                  </span>
                ))}
              </div>
              <div className="rounded-2xl bg-slate-900 px-4 py-3 text-white">
                <p className="text-sm text-white/70">Trạng thái</p>
                <p className="text-xl font-black">
                  Sẵn sàng chơi • Đã trừ 45.000đ
                </p>
              </div>
            </div>
            <div className="flex flex-col items-center justify-center gap-6 rounded-3xl bg-linear-to-br from-orange-400 to-pink-500 p-6 text-white shadow-lg">
              <p className="text-sm uppercase tracking-[0.4em] text-white/70">
                Countdown
              </p>
              <CountdownPulse />
              <p className="text-lg font-semibold text-center text-white/90">
                Hiệu ứng đếm ngược 3 → 1 → Start giúp bé biết khi nào trò chơi
                bắt đầu.
              </p>
            </div>
          </div>
        </Section>
      </div>
    </div>
  );
}

function IconBadge({ name }: { name: IconName }) {
  return (
    <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-white text-sky-600 shadow-inner shadow-sky-100">
      <IconSvg name={name} className="h-8 w-8" />
    </div>
  );
}

function Section({
  id,
  badge,
  title,
  subtitle,
  children,
}: {
  id: string;
  badge: string;
  title: string;
  subtitle: string;
  children: React.ReactNode;
}) {
  return (
    <section
      id={id}
      className="rounded-4xl bg-white/95 p-8 shadow-xl shadow-slate-200 ring-1 ring-white"
    >
      <div className="flex flex-col gap-2">
        <span className="text-xs font-bold uppercase tracking-[0.4em] text-sky-500">
          {badge}
        </span>
        <h2 className="text-3xl font-black text-slate-900">{title}</h2>
        <p className="text-base text-slate-500">{subtitle}</p>
      </div>
      <div className="mt-6 space-y-6">{children}</div>
    </section>
  );
}

function CountdownPulse() {
  const steps = ["3", "2", "1", "Start"];
  const [current, setCurrent] = useState(0);

  useEffect(() => {
    const interval = setInterval(() => {
      setCurrent((prev) => (prev + 1) % steps.length);
    }, 900);
    return () => clearInterval(interval);
  }, [steps.length]);

  return (
    <div className="relative flex h-40 w-40 items-center justify-center rounded-full border-4 border-white/50 bg-white/10 text-5xl font-black shadow-[0_20px_45px_rgba(249,115,22,0.5)]">
      {steps[current]}
      <div className="absolute inset-3 rounded-full border-2 border-dashed border-white/40" />
      <div className="absolute inset-6 animate-ping rounded-full border border-white/30" />
    </div>
  );
}

function IconSvg({
  name,
  className = "",
}: {
  name: IconName;
  className?: string;
}) {
  const shared = `stroke-current ${className}`;
  switch (name) {
    case "games":
      return (
        <svg
          viewBox="0 0 48 48"
          className={shared}
          fill="none"
          strokeWidth={2.5}
        >
          <rect x="5" y="16" width="38" height="20" rx="10" />
          <path d="M14 26h6m-3-3v6" />
          <circle cx="33" cy="24" r="3" />
          <circle cx="38" cy="28" r="2" />
        </svg>
      );
    case "cart":
      return (
        <svg
          viewBox="0 0 48 48"
          className={shared}
          fill="none"
          strokeWidth={2.5}
        >
          <path d="M10 12h4l4 20h18l4-14H16" />
          <circle cx="20" cy="38" r="2.5" />
          <circle cx="34" cy="38" r="2.5" />
        </svg>
      );
    case "card":
      return (
        <svg
          viewBox="0 0 48 48"
          className={shared}
          fill="none"
          strokeWidth={2.5}
        >
          <rect x="6" y="12" width="36" height="24" rx="4" />
          <path d="M6 20h36" />
          <path d="M14 28h8" />
        </svg>
      );
    case "wallet":
      return (
        <svg
          viewBox="0 0 48 48"
          className={shared}
          fill="none"
          strokeWidth={2.5}
        >
          <rect x="6" y="14" width="36" height="20" rx="5" />
          <path d="M34 20h8v8h-8a4 4 0 0 1-4-4v0a4 4 0 0 1 4-4z" />
          <circle cx="36.5" cy="24" r="1.5" />
        </svg>
      );
    case "play":
      return (
        <svg
          viewBox="0 0 48 48"
          className={shared}
          fill="none"
          strokeWidth={2.5}
        >
          <circle cx="24" cy="24" r="16" />
          <path d="M21 18v12l10-6-10-6z" fill="currentColor" stroke="none" />
        </svg>
      );
    case "spark":
      return (
        <svg viewBox="0 0 24 24" className={shared} fill="none" strokeWidth={2}>
          <path d="M12 3v6m0 6v6m-5-5h6m4-2h4m-4-4h4M5 5l4 4m-4 6 4-4" />
        </svg>
      );
    case "qr":
      return (
        <svg
          viewBox="0 0 48 48"
          className={shared}
          fill="none"
          strokeWidth={2.5}
        >
          <path d="M10 10h10v10H10zM28 10h10v10H28zM10 28h10v10H10z" />
          <path d="M28 28h4v10m6-10v4h-6m0-8h10" />
        </svg>
      );
    case "atm":
      return (
        <svg
          viewBox="0 0 48 48"
          className={shared}
          fill="none"
          strokeWidth={2.5}
        >
          <rect x="6" y="12" width="36" height="8" rx="2" />
          <path d="M14 20v16h20V20" />
          <path d="M20 26h8m-8 6h8" />
        </svg>
      );
    case "ewallet":
      return (
        <svg
          viewBox="0 0 48 48"
          className={shared}
          fill="none"
          strokeWidth={2.5}
        >
          <rect x="10" y="12" width="28" height="24" rx="4" />
          <path d="M14 18h20m-12 6h12m-12 6h8" />
        </svg>
      );
    case "nfc":
      return (
        <svg viewBox="0 0 64 64" className={shared} fill="none" strokeWidth={3}>
          <rect x="14" y="10" width="36" height="44" rx="6" />
          <path d="M26 22v20l12-8V22l-12 8" />
          <path d="M8 22c0 10 0 20 8 20m40-20c0 10 0 20-8 20" />
        </svg>
      );
    default:
      return null;
  }
}
