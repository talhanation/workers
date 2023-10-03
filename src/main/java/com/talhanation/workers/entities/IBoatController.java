package com.talhanation.workers.entities;

import com.talhanation.smallships.world.entity.ship.Ship;
import com.talhanation.smallships.world.entity.ship.abilities.Sailable;
import com.talhanation.workers.Main;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.phys.Vec3;


public interface IBoatController {

    default AbstractWorkerEntity getWorker() {
        return (AbstractWorkerEntity) this;
    }

    BlockPos getSailPos();

    float getPrecisionMin();
    float getPrecisionMax();
    void setSailPos(BlockPos pos);

    default void updateBoatControl(double posX, double posZ, double speedFactor, double turnFactor, Node node){
        if(this.getWorker().getVehicle() instanceof Boat boat && boat.getPassengers().get(0).equals(this.getWorker())) {
            String string = boat.getEncodeId();
            if(Main.isSmallShipsInstalled && (string.contains("smallships"))){
                if(this.getWorker() instanceof MerchantEntity merchant && getWaterDepth(boat.getOnPos()) >= 7 && merchant.getCurrentWayPoint() != null && getWaterDepth(merchant.getCurrentWayPoint()) >= 7 && !merchant.getFollow() && !boat.horizontalCollision){
                    updateSmallShipsBoatControl(boat, merchant.getCurrentWayPoint().getX(), merchant.getCurrentWayPoint().getZ(), true);
                }
                else if(this.getWorker() instanceof MerchantEntity merchant && getWaterDepth(boat.getOnPos()) >= 7 && merchant.getCurrentWayPoint() != null  && !merchant.getFollow() && !boat.horizontalCollision){
                    updateSmallShipsBoatControl(boat, merchant.getCurrentWayPoint().getX(), merchant.getCurrentWayPoint().getZ(), false);
                }
                else
                    updateSmallShipsBoatControl(boat, posX, posZ, false);
            }
            else
                updateVanillaBoatControl(boat, posX, posZ, speedFactor, turnFactor);
        }
    }

    default void updateVanillaBoatControl(Boat boat, double posX, double posZ, double speedFactor, double turnFactor){
        Vec3 forward = boat.getForward().yRot(-90).normalize();
        Vec3 target = new Vec3(posX, 0, posZ);
        Vec3 toTarget = boat.position().subtract(target).normalize();

        double phi = horizontalAngleBetweenVectors(forward, toTarget);
        //Main.LOGGER.info("phi: " + phi);
        double reff = 63.5F;
        boolean inputLeft =  (phi < reff);
        boolean inputRight = (phi > reff);
        boolean inputUp = Math.abs(phi - reff) <= reff * 0.15F;

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


    default void updateSmallShipsBoatControl(Boat boat, double posX, double posZ, boolean fast) {
        Vec3 forward = boat.getForward().yRot(-90).normalize();
        Vec3 target = new Vec3(posX, 0, posZ);
        Vec3 toTarget = boat.position().subtract(target).normalize();

        double phi = horizontalAngleBetweenVectors(forward, toTarget);
        //Main.LOGGER.info("phi: " + phi);
        double reff = 63.5F;
        boolean inputLeft =  (phi < reff);
        boolean inputRight = (phi > reff);
        boolean inputUp = Math.abs(phi - reff) <= reff * 0.35F;

        float boatSpeed = 0;
        float boatRotSpeed = 0;
        float maxRotSp = 2.0F;
        float rotAcceleration = 0.35F;
        float acceleration = 0.005F;
        float setPoint = 0;

        /*
        try {
            Class<?> shipClass = Class.forName("talhnation.smallships.world.entity.ship.Ship");
            if(shipClass.isInstance(boat)){
                Object ship = shipClass.cast(boat);

                Method getSpeedMethod = ship.getClass().getMethod("getSpeed", float.class);
                boatSpeed = getSpeedMethod.
            }
        } catch (ClassNotFoundException e) {
            Main.LOGGER.info("smallShipsShipClass was not found");
        } catch (NoSuchMethodException e) {
            Main.LOGGER.info("setSpeedMethod was not found");
        } catch (InvocationTargetException | IllegalAccessException e) {
            Main.LOGGER.info("setSpeedMethod could not invocation");
        }
        */

        if(boat instanceof Ship ship) {
            boatSpeed = ship.getSpeed();
            boatRotSpeed = ship.getRotSpeed();

            //TODO Sync ship state:  updateControls(((BoatAccessor) this).isInputUp(),((BoatAccessor) this).isInputDown(), ((BoatAccessor) this).isInputLeft(), ((BoatAccessor) this).isInputRight(), player);
            ship.updateControls(inputUp, false, inputLeft, inputRight, null); //Player parameter can be null because its not client side

            //TODO if(this.isInWater() && !((BoatLeashAccess) this).isLeashed()){


            if (inputUp) {
                double distance = toTarget.distanceToSqr(boat.position());
                byte state = 3;
                if(fast){
                    state = 4;
                    setPoint = 0.3F;
                }
                else if(distance > 20){
                    setPoint = 0.15F;
                }
                else{
                    setPoint = 0.075F;
                }
                if(ship instanceof Sailable sailable){
                    //float speedInKmH = Kalkuel.getKilometerPerHour(boatSpeed);
                    byte currentSail = sailable.getSailState();
                    if(currentSail != state) sailable.setSailState((byte) state);
                }
            }
            else {
                if(ship instanceof Sailable sailable){
                    //float speedInKmH = Kalkuel.getKilometerPerHour(boatSpeed);
                    byte currentSail = sailable.getSailState();
                    if(currentSail != 1) sailable.setSailState((byte) 1);
                }
                setPoint = 0.025F;
            }

            this.calculateSpeed(boat, boatSpeed, acceleration, setPoint);

            //CALCULATE ROTATION SPEED//
            float rotationSpeed = subtractToZero(boatRotSpeed, getVelocityResistance() * 2.5F);


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


            ship.setRotSpeed(rotationSpeed);

            boat.deltaRotation = rotationSpeed;
            boat.setYRot(boat.getYRot() + boat.deltaRotation);

            //SET
            boat.setDeltaMovement(calculateMotionX(boatSpeed, boat.getYRot()), 0.0F, calculateMotionZ(boatSpeed, boat.getYRot()));
            //}

        }
    }

    default float getVelocityResistance(){

        return 0.007F;

    }

    //Taken from Smallships/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private void calculateSpeed(Boat boat, float speed, float acceleration, float setPoint) {
        if (speed < setPoint) {
            speed = addToSetPoint(speed, acceleration, setPoint);
        } else
            speed = subtractToZero(speed, getVelocityResistance() * 2.2F);

        if(boat instanceof Ship ship) {
            ship.setSpeed(speed);
        }

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

    private double horizontalAngleBetweenVectors(Vec3 vector1, Vec3 vector2) {
        double dotProduct = vector1.x * vector2.x + vector1.z * vector2.z;
        double magnitude1 = Math.sqrt(vector1.x * vector1.x + vector1.z * vector1.z);
        double magnitude2 = Math.sqrt(vector2.x * vector2.x + vector2.z * vector2.z);

        double cosTheta = dotProduct / (magnitude1 * magnitude2);

        return Math.toDegrees(Math.acos(cosTheta));
    }

    private int getWaterDepth(BlockPos pos){
        int depth = 0;
        for(int i = 0; i < 10; i++){
            BlockState state = getWorker().level.getBlockState(pos.below(i));
            if(state.is(Blocks.WATER)){
                depth++;
            }
            else break;
        }
        return depth;
    }
}
