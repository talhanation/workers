package com.talhanation.workers;

public enum WorkAreaTypes {
    CROPAREA(0),
    BUILDING(1),
    MARKET(2),
    STORAGE(3),
    ANIMAL_PEN(4),
    MINING(5),
    LUMBER(6),
    FISHING(7),
    HOME(8),
    KITCHEN(9);
    private final int index;
    WorkAreaTypes(int index){
        this.index = index;
    }

    public int getIndex(){
        return this.index;
    }

    public static WorkAreaTypes fromIndex(int index) {
        for (WorkAreaTypes workAreaTypes : WorkAreaTypes.values()) {
            if (workAreaTypes.getIndex() == index) {
                return workAreaTypes;
            }
        }
        throw new IllegalArgumentException("Invalid State index: " + index);
    }
}
