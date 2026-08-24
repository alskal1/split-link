import type { ReactNode } from "react";

function Layout({ children }: { children: ReactNode }) {
  return (
    <div className="min-h-screen w-full bg-[#fdf3eb]">
      <div className="mx-auto flex min-h-screen w-full max-w-screen-sm flex-col px-4 py-6 sm:max-w-screen-md sm:px-6 sm:py-8 md:max-w-screen-lg md:px-8 lg:max-w-screen-xl">
        {children}
      </div>
    </div>
  );
}

export default Layout;
