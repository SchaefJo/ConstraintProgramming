import org.jacop.constraints.*;
import org.jacop.core.IntVar;
import org.jacop.core.Store;
import org.jacop.search.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


public class urban_development {
    int MINIMUM = 0;
    int MAXIMUM = 1;

    /**
     * function that loops over an array and gets the min and max value
     * @param array integer array
     * @return array with 0 position the minimum and maximum at position 1
     */
    public static int[] min_max(int[] array){
        int cur_min = Integer.MAX_VALUE;
        int cur_max = Integer.MIN_VALUE;

        for (int i=0; i<array.length; i++){
            if (array[i] < cur_min){
                cur_min = array[i];
            }
            if (array[i] > cur_max){
                cur_max = array[i];
            }
        }
        return new int[]{cur_min, cur_max};
    }

    /**
     * method that creates all necessary constraints, searches for the solution and prints it
     * @param n number of rows/columns in our square
     * @param n_res number of residentials in the whole square
     * @param points point distribution ordered from 0 residentials to n residentials in one row/col
     */
    public void urban_planning(int n, int n_res, int[] points){
        double timeInitial = System.currentTimeMillis();

        Store store = new Store();
        // create a List of the points
        List<Integer> point_list = Arrays.stream(points).boxed().collect(Collectors.toList());

        // x is a flat array for the fields which are either 0 (commercial) or 1 (residential)
        IntVar[] x = new IntVar[n*n];
        for (int i=0; i<n*n; i++){
            x[i] = new IntVar(store, String.valueOf(i), 0, 1);
        }

        IntVar sum_res = new IntVar(store, "residential_sum", n_res, n_res);
        store.impose(new SumInt(x, "==", sum_res));

        int[] min_max_val = min_max(points);
        IntVar cost = new IntVar(store, "maximal_cost",min_max_val[MINIMUM]*n*n,
                min_max_val[MAXIMUM]*n*n);

        // create two dimensional representation of the board with integer division and modulo
        // the first dimension consists of the columns and rows
        IntVar[][] field = new IntVar[2*n][n];
        for (int i=0; i<n*n; i++){
            int x_cor = i/n;
            int y_cor = i%n;

            field[x_cor][y_cor] = x[i];
            field[n+y_cor][x_cor] = x[i];
        }

        // count_res counts the 1's in one column or row
        // scores counts the scores for all columns and rows
        IntVar[] count_res = new IntVar[2*n];
        IntVar[] scores = new IntVar[2*n];
        for (int i=0; i<n; i++){
            IntVar[] row = field[i];
            IntVar[] col = field[i+n];

            count_res[2*i] = new IntVar(store, 0, n);
            count_res[2*i+1] = new IntVar(store, 0, n);
            scores[2*i] = new IntVar(store, min_max_val[MINIMUM], min_max_val[MAXIMUM]);
            scores[2*i+1] = new IntVar(store, min_max_val[MINIMUM], min_max_val[MAXIMUM]);

            store.impose(new SumInt(row, "==", count_res[2*i]));
            store.impose(new SumInt(col, "==", count_res[2*i+1]));
            // match number of residential buildings with the points
            store.impose(new ElementInteger(count_res[2*i], point_list, scores[2*i], -1));
            store.impose(new ElementInteger(count_res[2*i+1], point_list, scores[2*i+1], -1));
        }

        store.impose(new SumInt(scores, "==", cost));

        //symmetry breaking constraint
        for (int i = 0; i<n-1; i++) {
            // lt needs to be set to false since in our case we allow "<=" compared to "<"
            store.impose(new LexOrder(field[i], field[i+1], false));
            store.impose(new LexOrder(field[i+n], field[i+n+1], false));
        }

        // instead of maximizing the cost, minimize the negation of the cost
        IntVar min_cost = new IntVar(store, min_max_val[MINIMUM]*n*n, min_max_val[MAXIMUM]*n*n);
        store.impose(new XplusYeqC(cost, min_cost, 0));

        Search<IntVar> label = new DepthFirstSearch<IntVar>();
        label.setPrintInfo(true);
        SelectChoicePoint<IntVar> select =
                new SimpleSelect<IntVar>(
                        x,
                        new SmallestDomain<IntVar>(),
                        new IndomainMin<IntVar>());
        boolean result = label.labeling(store, select, min_cost);

        double timeFinal = System.currentTimeMillis();
        System.out.println("Runtime: "+(timeFinal-timeInitial)+"ms");

        for (int i=0; i<n; i++){
            System.out.println(Arrays.toString(field[i]));
        }
        System.out.println(cost);
    }


    public static void main(String[] args) {
        int data_set = Integer.parseInt(args[0]);

        int n, n_residential;
        int[] point_distribution;

        if (data_set==0){
            n = 5;
            n_residential = 12;
            point_distribution = new int[]{-5, -4, -3, 3, 4, 5};
        } else if (data_set==1){
            n = 5;
            n_residential = 18;
            point_distribution = new int[]{-5, -4, -3, 3, 4, 5};
        } else if (data_set==2){
            n = 7;
            n_residential = 29;
            point_distribution = new int[]{-7, -6, -5, -4, 4, 5, 6, 7};
        } else if (data_set==3){
            n = 5;
            n_residential = 0;
            point_distribution = new int[]{-5, -4, -3, 3, 4, 5};
        } else if (data_set==4){
            n = 5;
            n_residential = 25;
            point_distribution = new int[]{-5, -4, -3, 3, 4, 5};
        } else if (data_set==5){
            n = 5;
            n_residential = 20;
            point_distribution = new int[]{-5, -4, -3, 3, 4, 5};
        } else {
            n = 5;
            n_residential = 3;
            point_distribution = new int[]{-5, -4, -3, 3, 4, 5};
        }

        urban_development ud = new urban_development();
        ud.urban_planning(n, n_residential, point_distribution);
    }
}
