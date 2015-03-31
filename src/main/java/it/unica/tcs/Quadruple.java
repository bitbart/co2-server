package it.unica.tcs;

public class Quadruple<Fi, S, T, Fo> {

    private Fi      first;  // first member of quadruple
    private S       second; // second member of quadruple
    private T       third;  // third member of quadruple
    private Fo      fourth; // fourth member of quadruple

    private boolean isEmpty;

    public Quadruple() {

        isEmpty = true;
    }

    public Quadruple(Fi first, S second, T third, Fo fourth) {

        this.first = first;
        this.second = second;
        this.third = third;
        this.fourth = fourth;

        isEmpty = false;
    }

    public void set(Fi first, S second, T third, Fo fourth) {

        this.first = first;
        this.second = second;
        this.third = third;
        this.fourth = fourth;

        isEmpty = false;
    }

    public Fi getFirst() {

        return first;
    }

    public S getSecond() {

        return second;
    }

    public T getThird() {

        return third;
    }

    public Fo getFourth() {

        return fourth;
    }

    public boolean isEmpty() {

        return isEmpty;
    }
}
