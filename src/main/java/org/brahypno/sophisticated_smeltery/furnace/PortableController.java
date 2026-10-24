package org.brahypno.sophisticated_smeltery.furnace;

/** Marks a detached native controller; it is never registered or ticked in the level. */
public interface PortableController {
    void sophisticatedsmeltery$attach(Runnable changed);
    boolean sophisticatedsmeltery$isPortable();
    void sophisticatedsmeltery$changed();
    void sophisticatedsmeltery$setSizing(boolean sizing);
    boolean sophisticatedsmeltery$isSizing();
    int sophisticatedsmeltery$getFluidRevision();
}
