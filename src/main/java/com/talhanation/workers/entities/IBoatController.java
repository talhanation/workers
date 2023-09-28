package com.talhanation.workers.entities;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.vehicle.Boat;

public interface IBoatController {

    default AbstractWorkerEntity getWorker() {
        return (AbstractWorkerEntity) this;
    }

    BlockPos getSailPos();

    float getPrecisionMin();
    float getPrecisionMax();
    void setSailPos(BlockPos pos);

    default void updateBoatControl(double posX, double posZ, double speedFactor, double turnFactor){
        if(this.getWorker().getVehicle() instanceof Boat boat && boat.getPassengers().get(0).equals(this.getWorker())) {
            updateVanillaBoatControl(boat, posX, posZ, speedFactor, turnFactor);
        }
        //else if(Main.isSmallShipsInstalled)
    }

    default void updateVanillaBoatControl(Boat boat, double posX, double posZ, double speedFactor, double turnFactor){
        double dx = posX - this.getWorker().getX();
        double dz = posZ - this.getWorker().getZ();

        float angle = Mth.wrapDegrees((float) (Mth.atan2(dz, dx) * 180.0D / 3.14D) - 90.0F);
        float drot = angle - Mth.wrapDegrees(boat.getYRot());

        boolean inputLeft = (drot < 0.0F && Math.abs(drot) >= 2.5F);
        boolean inputRight = (drot > 0.0F && Math.abs(drot) >= 2.5F);
        boolean inputUp = (Math.abs(drot) < 25.0F);

        float f = 0.0F;

        if (inputLeft) {
            boat.setYRot((float) (boat.getYRot() - 2.5F));
        }

        if (inputRight) {
            boat.setYRot((float) (boat.getYRot() + 2.5F));
        }


        if (inputRight != inputLeft && !inputUp) {
            f += 0.005F * speedFactor;
        }

        if (inputUp) {
            f += 0.02F * speedFactor;
        }

        boat.setDeltaMovement(boat.getDeltaMovement().add((double)(Mth.sin(-boat.getYRot() * ((float)Math.PI / 180F)) * f), 0.0D, (double)(Mth.cos(boat.getYRot() * ((float)Math.PI / 180F)) * f)));
        boat.setPaddleState(inputRight || inputUp, inputLeft || inputUp);
    }

}
