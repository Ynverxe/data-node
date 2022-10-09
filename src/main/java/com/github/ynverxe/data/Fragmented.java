package com.github.ynverxe.data;

import org.jetbrains.annotations.NotNull;

public interface Fragmented {

    @NotNull DataNode defragment();
    
}