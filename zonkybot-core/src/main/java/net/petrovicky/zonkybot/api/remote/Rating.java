package net.petrovicky.zonkybot.api.remote;

public enum Rating {

    AAAAA("A**"),
    AAAA("A*"),
    AAA("A++"),
    AA("A+"),
    A("A"),
    B("B"),
    C("C"),
    D("D");


    private final String description;

    Rating(final String description) {
        this.description = description;
    }

    public String getDescription() {
        return this.description;
    }

}
