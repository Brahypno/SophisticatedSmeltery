package org.brahypno.sophisticated_smeltery;

import net.minecraftforge.common.ForgeConfigSpec;

/** Local UI preferences, read when opening the storage screen. */
public final class ClientConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.IntValue TINKER_STATION_TOOL_ROWS;

    static {
        var builder = new ForgeConfigSpec.Builder();
        builder.push("tinkerStation");
        TINKER_STATION_TOOL_ROWS = builder
                .comment("Visible tool-selection rows for both Tinker Station and Tinkers' Anvil upgrades.",
                        "Each row holds five tools; scroll to access the remaining tools.",
                        "Fewer rows move the crafting controls up and shorten the panel.",
                        "Reopen the storage screen after changing this setting.")
                .defineInRange("toolSelectionRows", 3, 1, 5);
        builder.pop();
        SPEC = builder.build();
    }

    private ClientConfig() { }
}
