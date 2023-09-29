package com.talhanation.workers.entities;

import com.talhanation.workers.Main;
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
            String string = boat.getEncodeId();
            if(Main.isSmallShipsInstalled && (string.equals("smallships:cog") || string.equals("smallships:brigg"))){
                updateSmallShipsBoatControl(boat, posX, posZ);
            }
            else
                updateVanillaBoatControl(boat, posX, posZ, speedFactor, turnFactor);
        }
    }

    default void updateVanillaBoatControl(Boat boat, double posX, double posZ, double speedFactor, double turnFactor){
        double dx = posX - boat.getX();
        double dz = posZ - boat.getZ();

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


    default void updateSmallShipsBoatControl(Boat boat, double posX, double posZ) {
        /*
        double dx = posX - boat.getX();
        double dz = posZ - boat.getZ();

        float angle = Mth.wrapDegrees((float) (Mth.atan2(dz, dx) * 180.0D / 3.14D) - 90.0F);
        float drot = angle - Mth.wrapDegrees(boat.getYRot());

        boolean inputLeft = (drot < 0.0F && Math.abs(drot) >= 2.5F);
        boolean inputRight = (drot > 0.0F && Math.abs(drot) >= 2.5F);
        boolean inputUp = (Math.abs(drot) < 25.0F);

        float boatSpeed = boat.getSpeed();//TODO: REFLECTION NEEDED
        float maxRotSp = 0.5F;
        float rotAcceleration = 0.1F;
        //TODO Sync ship state:  updateControls(((BoatAccessor) this).isInputUp(),((BoatAccessor) this).isInputDown(), ((BoatAccessor) this).isInputLeft(), ((BoatAccessor) this).isInputRight(), player);

        //TODO if(this.isInWater() && !((BoatLeashAccess) this).isLeashed()){

        if (inputUp) {
            float acceleration = 0.1F;
            float setPoint = 4F; //SET POINT DEPENDENT ON DISTANCE TO NEXT WAYPOINT;
        }
        this.calculateSpeed(boat, boatSpeed, acceleration, setPoint);
        //TODO: SET SAIL STATE DEPENDENT ON SPEED

        //switch (speed){

        //}



        //CALCULATE ROTATION SPEED//
        float rotationSpeed = subtractToZero(getRotSpeed(), getVelocityResistance() * 2.5F);


        if (inputRight) {
            if (rotationSpeed < maxRotSp) {
                rotationSpeed = Math.min(rotationSpeed + rotAcceleration * 1 / 8, maxRotSp);
            }
        }

        if (inputLeft) {
            if (rotationSpeed > -maxRotSp) {
                rotationSpeed = Math.max(rotationSpeed - rotAcceleration * 1 / 8, -maxRotSp);
            }
        }



        //boat.setRotSpeed(rotationSpeed);

        boat.setDeltaRotation(rotationSpeed);
        boat.setYRot(boat.getYRot() + boat.getDeltaRotation());

        //SET
        boat.setDeltaMovement(calculateMotionX(boatSpeed, this.getYRot()), 0.0F, calculateMotionZ(boatSpeed, this.getYRot()));
        //}


         */
    }

    //Taken from Smallships/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private void calculateSpeed(Boat boat, float speed, float acceleration, float setPoint) {
        // If there is no interaction the speed should get reduced

        if(speed < setPoint){
            speed = addToSetPoint(speed, acceleration, setPoint);
        }
        //else
          //  speed = subtractToZero(speed, getVelocityResistance() * 0.8F);//TODO: REFLECTION NEEDED

        //boat.setSpeed(speed);//TODO: REFLECTION NEEDED
    }

    /**
     * Adds from the provided number, but does not cross the set point
     *
     * @param current the current number
     * @param positiveChange the amount to add
     * @param setPoint the amount to not cross
     * @return the resulting number
     */
    private float addToSetPoint(float current, float positiveChange, float setPoint) {
        if (current < setPoint) {
            current = current + positiveChange;
        }
        return current;
    }


    private double calculateMotionX(float speed, float rotationYaw) {
        return Mth.sin(-rotationYaw * 0.017453292F) * speed;
    }

    private double calculateMotionZ(float speed, float rotationYaw) {
        return Mth.cos(rotationYaw * 0.017453292F) * speed;
    }

    /**
     * Subtracts from the provided number, but does not cross zero
     *
     * @param num the number
     * @param sub the amount to subtract
     * @return the resulting number
     */
    private float subtractToZero(float num, float sub) {
        float erg;
        if (num < 0F) {
            erg = num + sub;
            if (erg > 0F) {
                erg = 0F;
            }
        }
        else {
            erg = num - sub;
            if (erg < 0F) {
                erg = 0F;
            }
        }
        return erg;
    }
}
