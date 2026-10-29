package io.github.lukmnh.toolbox.devutils.regex.infrastructure;

/** Posisi grup pada match saat ini (grup 0 = seluruh match); -1 bila grup tidak ikut cocok. */
interface GroupBounds {

    int start(int group);

    int end(int group);
}
