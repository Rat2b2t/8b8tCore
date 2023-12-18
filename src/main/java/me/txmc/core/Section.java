package me.txmc.core;

/**
 * @author 254n_m
 * @since 2023/12/17 9:11 PM
 * This file was created as a part of 8b8tCore
 */
public interface Section {
    void enable();

    void disable();

    void reloadConfig();

    String getName();
}
