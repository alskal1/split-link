import { Outlet } from "react-router-dom";

function MobileLayout() {
  return (
    <div className="flex min-h-dvh w-full justify-center bg-[#fdf3eb]">
      <div className="flex min-h-dvh w-full max-w-[480px] flex-col px-5 py-6 [padding-bottom:max(1.5rem,env(safe-area-inset-bottom))] [padding-top:max(1.5rem,env(safe-area-inset-top))]">
        <Outlet />
      </div>
    </div>
  );
}

export default MobileLayout;
