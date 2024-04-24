package com.mrmelon54.infrastructury.networking;

import io.netty.buffer.Unpooled;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import remapped.architectury.extensions.network.EntitySpawnExtension;

public class SpawnEntityPacket {
    private static final ResourceLocation PACKET_ID = new ResourceLocation("infrastructury", "spawn_entity_packet");

    public static Packet<ClientGamePacketListener> create(Entity entity) {
        if (entity.level().isClientSide()) {
            throw new IllegalStateException("SpawnEntityPacket.create called on the logical client!");
        }
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        buffer.writeVarInt(BuiltInRegistries.ENTITY_TYPE.getId(entity.getType()));
        buffer.writeUUID(entity.getUUID());
        buffer.writeVarInt(entity.getId());
        var position = entity.position();
        buffer.writeDouble(position.x);
        buffer.writeDouble(position.y);
        buffer.writeDouble(position.z);
        buffer.writeFloat(entity.getXRot());
        buffer.writeFloat(entity.getYRot());
        buffer.writeFloat(entity.getYHeadRot());
        var deltaMovement = entity.getDeltaMovement();
        buffer.writeDouble(deltaMovement.x);
        buffer.writeDouble(deltaMovement.y);
        buffer.writeDouble(deltaMovement.z);
        if (entity instanceof EntitySpawnExtension ext) {
            ext.saveAdditionalSpawnData(buffer);
        }
        return (Packet<ClientGamePacketListener>) NetworkManager.toPacket(NetworkManager.Side.S2C, PACKET_ID, buffer);
    }


    @Environment(EnvType.CLIENT)
    public static class Client {
        @Environment(EnvType.CLIENT)
        public static void register() {
            NetworkManager.registerReceiver(NetworkManager.Side.S2C, PACKET_ID, Client::receive);
        }

        @Environment(EnvType.CLIENT)
        public static void receive(FriendlyByteBuf buf, NetworkManager.PacketContext context) {
            var entityTypeId = buf.readVarInt();
            var uuid = buf.readUUID();
            var id = buf.readVarInt();
            var x = buf.readDouble();
            var y = buf.readDouble();
            var z = buf.readDouble();
            var xRot = buf.readFloat();
            var yRot = buf.readFloat();
            var yHeadRot = buf.readFloat();
            var deltaX = buf.readDouble();
            var deltaY = buf.readDouble();
            var deltaZ = buf.readDouble();
            // Retain this buffer, so we can use it in the queued task (EntitySpawnExtension)
            buf.retain();
            context.queue(() -> {
                var entityType = BuiltInRegistries.ENTITY_TYPE.byId(entityTypeId);
                if (entityType == null) {
                    throw new IllegalStateException("Entity type (" + entityTypeId + ") is unknown, spawning at (" + x + ", " + y + ", " + z + ")");
                }
                if (Minecraft.getInstance().level == null) {
                    throw new IllegalStateException("Client world is null!");
                }
                var entity = entityType.create(Minecraft.getInstance().level);
                if (entity == null) {
                    throw new IllegalStateException("Created entity is null!");
                }
                entity.setUUID(uuid);
                entity.setId(id);
                entity.syncPacketPositionCodec(x, y, z);
                entity.moveTo(x, y, z);
                entity.setXRot(xRot);
                entity.setYRot(yRot);
                entity.setYHeadRot(yHeadRot);
                entity.setYBodyRot(yHeadRot);
                if (entity instanceof EntitySpawnExtension ext) {
                    ext.loadAdditionalSpawnData(buf);
                }
                buf.release();
                Minecraft.getInstance().level.addEntity(entity);
                entity.lerpMotion(deltaX, deltaY, deltaZ);
            });
        }
    }
}
