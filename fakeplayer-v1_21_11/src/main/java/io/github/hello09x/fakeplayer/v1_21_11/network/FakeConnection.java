package io.github.hello09x.fakeplayer.v1_21_11.network;

import io.github.hello09x.fakeplayer.core.network.FakeChannel;
import io.netty.channel.ChannelFutureListener;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetAddress;

public class FakeConnection extends Connection {

    public FakeConnection(@NotNull InetAddress address) {
        super(PacketFlow.SERVERBOUND);
        this.channel = new FakeChannel(null, address);
        this.address = this.channel.remoteAddress();
        Connection.configureSerialization(this.channel.pipeline(), PacketFlow.SERVERBOUND, false, null);
    }

    @Override
    public boolean isConnected() {
        return super.isConnected();
    }

    @Override
    public void send(Packet<?> packet, @Nullable ChannelFutureListener channelfuturelistener) {
        this.handleSendListener(channelfuturelistener);
    }

    @Override
    public void send(Packet<?> packet, @Nullable ChannelFutureListener channelfuturelistener, boolean flag) {
        this.handleSendListener(channelfuturelistener);
    }

    @Override
    public void send(Packet<?> packet) {

    }

    private void handleSendListener(@Nullable ChannelFutureListener listener) {
        if (listener == null) {
            return;
        }

        try {
            listener.operationComplete(this.channel.newSucceededFuture());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
