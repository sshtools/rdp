package com.sshtools.javardp.layers;

public interface Layer<P extends Layer<?>> {
	P getParent();
}
