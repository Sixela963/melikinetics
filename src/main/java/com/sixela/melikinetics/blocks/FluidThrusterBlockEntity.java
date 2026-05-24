package com.sixela.melikinetics.blocks;

import com.sixela.melikinetics.MelikineticsMod;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.physics.force.ForceGroups;
import dev.ryanhcode.sable.api.physics.force.QueuedForceGroup;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.joml.Vector3d;

public class FluidThrusterBlockEntity extends BlockEntity implements BlockEntitySubLevelActor {

    public ThrusterFluidHandler fluidHandler;

    private boolean powered = false;

    //Weird sticky behaviour variables, because otherwise the create mechanical pump starts acting weird
    private static int tankCooldown = 10; //in ticks

    //internal tanks
    private FluidStack[] tank = {FluidStack.EMPTY,FluidStack.EMPTY,FluidStack.EMPTY};
    private FluidStack[] tankThrustBuffer = {FluidStack.EMPTY,FluidStack.EMPTY,FluidStack.EMPTY};
    private int tankCooldownCounter = 10;

    private double thrustMultiplierFromFluids = 0d;
    private double baseThrustMultiplier = 1d;


    public FluidThrusterBlockEntity(BlockPos pos, BlockState blockState) {
        super(MelikineticsMod.FLUID_THRUSTER_BLOCK_ENTITY.get(), pos, blockState);
        fluidHandler = new ThrusterFluidHandler();
    }

    public static void tick(Level level, BlockPos blockPos, BlockState blockState, FluidThrusterBlockEntity fluidThrusterBlockEntity) {


        if (level.isClientSide) {
            return;
        }

        for (int i = 0; i<fluidThrusterBlockEntity.fluidHandler.getTanks();i++) {
            fluidThrusterBlockEntity.tankThrustBuffer[i] = fluidThrusterBlockEntity.tank[i].copy();
            fluidThrusterBlockEntity.tank[i]=FluidStack.EMPTY;

        }

        if (level.isClientSide) {
            return;
        }

        fluidThrusterBlockEntity.powered = fluidThrusterBlockEntity.tankCooldownCounter<tankCooldown;

        if (fluidThrusterBlockEntity.powered) {
            fluidThrusterBlockEntity.tankCooldownCounter++;
            fluidThrusterBlockEntity.thrustMultiplierFromFluids = fluidThrusterBlockEntity.getThrustMultiplierFromFluidsInTanks();
            /*MelikineticsMod.LOGGER.info(String.format("[tick %d]thrust mult is %f",
                    level.getGameTime(),
                    fluidThrusterBlockEntity.thrustMultiplierFromFluids));*/
        }else {
            fluidThrusterBlockEntity.thrustMultiplierFromFluids = 0d;
        }

        if (fluidThrusterBlockEntity.powered != blockState.getValue(FluidThrusterBlock.POWERED)) {
            level.setBlock(blockPos,blockState.setValue(FluidThrusterBlock.POWERED,fluidThrusterBlockEntity.powered),3);
            setChanged(level,blockPos,blockState);
        }
    }

    private double getThrustMultiplierFromFluidsInTanks() {
        double result = 0d;
        result += tankThrustBuffer[0].getAmount();
        result += tankThrustBuffer[1].getAmount();
        result += tankThrustBuffer[2].getAmount();
        return result;
    }

    @Override
    public void sable$physicsTick(ServerSubLevel subLevel, RigidBodyHandle handle, double timeStep) {

        if (getBlockState().getBlock() != MelikineticsMod.FLUID_THRUSTER.get()) {
            return;
        }

        if (!powered)
            return;

        Vec3 thrust = Vec3.atLowerCornerOf(getBlockState().getValue(FluidThrusterBlock.FACING).getNormal());
        thrust = thrust.scale(-timeStep*baseThrustMultiplier*thrustMultiplierFromFluids);
        Vec3 thrustPos = Vec3.atCenterOf(getBlockPos());

        final QueuedForceGroup forceGroup = subLevel.getOrCreateQueuedForceGroup(ForceGroups.PROPULSION.get());

        forceGroup.applyAndRecordPointForce( new Vector3d(thrustPos.x,thrustPos.y,thrustPos.z),new Vector3d(thrust.x,thrust.y,thrust.z));
    }

    public class ThrusterFluidHandler implements IFluidHandler {

        @Override
        public int getTanks() {
            return tank.length;
        }

        @Override
        public FluidStack getFluidInTank(int i) {
            //return FluidStack.EMPTY;
            return tank[i];
        }

        @Override
        public int getTankCapacity(int i) {
            return 1000;
        }

        @Override
        public boolean isFluidValid(int i, FluidStack fluidStack) {
            return true;
        }

        // Ugly aaaa custom implementation for fluid tanks. that shit's terrible but it works
        @Override
        public int fill(FluidStack fluidStack, FluidAction fluidAction) {

            for (int i=0;i<getTanks();i++) {
                if (tank[i].isEmpty() || (FluidStack.isSameFluidSameComponents(tank[i],fluidStack)) && tank[i].getAmount()<getTankCapacity(i)) {

                    int amountToConsume = Integer.min(fluidStack.getAmount(),(getTankCapacity(i)-tank[i].getAmount()));
                    if (!fluidAction.simulate()) {
                        tank[i] = fluidStack.copyWithAmount(tank[i].getAmount()+amountToConsume);
                        tankCooldownCounter = 0;
                    }
                    return amountToConsume;
                }
            }

            return 0;
        }

        @Override
        public FluidStack drain(FluidStack fluidStack, FluidAction fluidAction) {
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int i, FluidAction fluidAction) {
            return FluidStack.EMPTY;
        }
    }
}
