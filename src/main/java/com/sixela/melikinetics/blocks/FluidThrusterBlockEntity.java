package com.sixela.melikinetics.blocks;

import com.sixela.melikinetics.MelikineticsMod;
import dev.ryanhcode.sable.ActiveSableCompanion;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.physics.force.ForceGroups;
import dev.ryanhcode.sable.api.physics.force.QueuedForceGroup;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

public class FluidThrusterBlockEntity extends BlockEntity implements BlockEntitySubLevelActor {

    public FluidThrusterBlockEntity(BlockPos pos, BlockState blockState) {
        super(MelikineticsMod.FLUID_THRUSTER_BLOCK_ENTITY.get(), pos, blockState);
    }

    @Override
    public void sable$physicsTick(ServerSubLevel subLevel, RigidBodyHandle handle, double timeStep) {
        ActiveSableCompanion helper = Sable.HELPER;

        if (getBlockState().getBlock() != MelikineticsMod.FLUID_THRUSTER.get()) {
            return;
        }
        Vec3 thrust = Vec3.atLowerCornerOf(getBlockState().getValue(FluidThrusterBlock.FACING).getNormal());
        thrust = thrust.scale(timeStep*10d);
        Vec3 thrustPos = Vec3.atCenterOf(getBlockPos());

        final QueuedForceGroup forceGroup = subLevel.getOrCreateQueuedForceGroup(ForceGroups.PROPULSION.get());

        forceGroup.applyAndRecordPointForce( new Vector3d(thrustPos.x,thrustPos.y,thrustPos.z),new Vector3d(thrust.x,thrust.y,thrust.z));
    }
}
