package com.cutting.cuttingsystem.model;

import com.cutting.cuttingsystem.entitys.algorithm.Instance;
import com.cutting.cuttingsystem.entitys.algorithm.Solution;
import com.cutting.cuttingsystem.entitys.algorithm.Square;
import com.cutting.cuttingsystem.util.ReadDataUtil;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AlgorithmUnitTest {

    @Test
    void evaluatePlacesRectangleWhenItFits() throws Exception {
        Instance instance = instance(10, 10, false, 0,
                List.of(new Square("square-1", 5, 5)));

        Solution solution = new TabuSearch(instance).evaluate(instance.getSquareList());

        assertEquals(1, solution.getPlaceSquareList().size());
        assertEquals(0.25, solution.getRate(), 0.000001);
        assertEquals(0, solution.getPlaceSquareList().get(0).getX(), 0.000001);
        assertEquals(0, solution.getPlaceSquareList().get(0).getY(), 0.000001);
    }

    @Test
    void evaluateRotatesRectangleWhenRotationIsEnabled() throws Exception {
        Instance instance = instance(5, 10, true, 0,
                List.of(new Square("square-1", 10, 5)));

        Solution solution = new TabuSearch(instance).evaluate(instance.getSquareList());

        assertEquals(1, solution.getPlaceSquareList().size());
        assertEquals(5, solution.getPlaceSquareList().get(0).getL(), 0.000001);
        assertEquals(10, solution.getPlaceSquareList().get(0).getW(), 0.000001);
        assertEquals(1.0, solution.getRate(), 0.000001);
    }

    @Test
    void evaluateDoesNotPlaceRectangleWhenGapMakesItTooLarge() throws Exception {
        Instance instance = instance(10, 10, false, 1,
                List.of(new Square("square-1", 9, 9)));

        Solution solution = new TabuSearch(instance).evaluate(instance.getSquareList());

        assertEquals(0, solution.getPlaceSquareList().size());
        assertEquals(0, solution.getRate(), 0.000001);
    }

    @Test
    void evaluateReturnsZeroRateForEmptySquareList() throws Exception {
        Instance instance = instance(10, 10, false, 0, List.of());

        Solution solution = new TabuSearch(instance).evaluate(instance.getSquareList());

        assertEquals(0, solution.getPlaceSquareList().size());
        assertEquals(0, solution.getRate(), 0.000001);
    }

    @Test
    void searchFindsPlacementForSingleRectangle() throws Exception {
        Instance instance = instance(10, 10, false, 0,
                List.of(new Square("square-1", 5, 5)));

        Solution solution = new TabuSearch(instance).search();

        assertEquals(1, solution.getPlaceSquareList().size());
        assertEquals(0.25, solution.getRate(), 0.000001);
    }

    @Test
    void getSolutionRemovesAllPackedSquaresWithSameSize() throws Exception {
        String json = """
                {
                  "L": 10,
                  "W": 10,
                  "rotateEnable": false,
                  "gapDistance": 0,
                  "squareList": [
                    { "id": "square-1", "l": 5, "w": 5 },
                    { "id": "square-2", "l": 5, "w": 5 }
                  ]
                }
                """;

        List<Solution> solutions = new ReadDataUtil().getSolution(json);

        assertEquals(1, solutions.size());
        assertEquals(2, solutions.get(0).getPlaceSquareList().size());
    }

    @Test
    void getSolutionFailsWhenNoSquareCanBePacked() {
        String json = """
                {
                  "L": 10,
                  "W": 10,
                  "rotateEnable": false,
                  "gapDistance": 0,
                  "squareList": [
                    { "id": "square-1", "l": 11, "w": 11 }
                  ]
                }
                """;

        assertThrows(IllegalStateException.class, () -> new ReadDataUtil().getSolution(json));
    }

    private Instance instance(double length, double width, boolean rotateEnable, double gapDistance, List<Square> squares) {
        Instance instance = new Instance();
        instance.setL(length);
        instance.setW(width);
        instance.setRotateEnable(rotateEnable);
        instance.setGapDistance(gapDistance);
        instance.setSquareList(squares);
        return instance;
    }
}
