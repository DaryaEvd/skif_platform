#include "../include/output.h"

#include "../include/variables.h"
#include "../cJSON/cJSON.h"
#include <stdio.h>
#include <stdlib.h>
//#include <windows.h>
#include <math.h>
#include <stdint.h>

extern double detArray[988][988], detArray_squared[988][988];
extern double max_dA, min_dA;
extern uint64_t N;
extern int det1, det2;
char buffer[8];

int output() {
	max_dA = log(max_dA);
	min_dA = log(min_dA);
	cJSON* json = cJSON_CreateObject();
	cJSON_AddNumberToObject(json, "max_dA", max_dA);
	cJSON_AddNumberToObject(json, "min_dA", min_dA);
	cJSON* img = cJSON_CreateArray();
	for (det1 = 0; det1 < 988; ++det1) {
		cJSON* row = cJSON_CreateArray();
		for (det2 = 0; det2 < 988; ++det2) {
			cJSON_AddItemToArray(row, cJSON_CreateNumber((detArray[det1][det2] != 0) ? log(detArray[det1][det2]) / max_dA : 0));
		}
		cJSON_AddItemToArray(img, row);
	}
	cJSON_AddItemToObject(json, "img", img);

	char* json_str = cJSON_Print(json);

    printf("you're just before end.json!!!");
	FILE* file = fopen("/output/end.json", "w");
	fflush(stdout);

	if (!file) {
//		printf("File opening error. File: \"end.json\".\n");

    perror("fopen /output/end.json failed");
		return 1;
	}

	fprintf(file, "%s\n", json_str);

    printf("you're after end.json!!!");
    fflush(stdout);

	cJSON_Delete(json);
	fclose(file);
	free(json_str);

	return 0;
}

int err_rate() {
	double max_err = 0, min_err = 10000, sum = 0, sum_sq = 0, intens_err, max_err_abs = 0, k;

	for (det1 = 0; det1 < 988; ++det1) {
		for (det2 = 0; det2 < 988; ++det2) {
			k = 3 * sqrt((detArray_squared[det1][det2] / (N * N)) - (detArray[det1][det2] * detArray[det1][det2]) / (N * N * N));
			max_err_abs = (max_err_abs > k) ? max_err_abs : k;
			if (detArray[det1][det2] != 0) {
				if (det1 != 493 && det1 != 494 && det2 != 493 && det2 != 494) sum += detArray[det1][det2];
				sum_sq += detArray_squared[det1][det2];
				detArray_squared[det1][det2] = (300 * sqrt(detArray_squared[det1][det2] - (detArray[det1][det2] * detArray[det1][det2]) / N)) / detArray[det1][det2];
				max_err = (max_err > detArray_squared[det1][det2]) ? max_err : detArray_squared[det1][det2];
				min_err = (min_err < detArray_squared[det1][det2]) ? min_err : detArray_squared[det1][det2];
				if (detArray_squared[det1][det2] < 5) printf("%lf\n", detArray[det1][det2]);
			}
		}
	}

	printf("%e, %lf\n", max_err_abs, sum / (988 * 988 - 4));

	intens_err = 300 * (sqrt(sum_sq - (sum * sum / N)) / (sum));

	cJSON* json = cJSON_CreateObject();
	cJSON_AddNumberToObject(json, "max_err_abs", max_err_abs);
	cJSON_AddNumberToObject(json, "mean", sum / (988 * 988));
	cJSON_AddNumberToObject(json, "max_err", max_err);
	cJSON_AddNumberToObject(json, "min_err", min_err);
	cJSON_AddNumberToObject(json, "inters_err", intens_err);
	char* json_str = cJSON_Print(json);
	cJSON_Delete(json);

	FILE* file = fopen("/json/error_rate.json", "w");
	fprintf(file, "%s", json_str);
	fclose(file);
	free(json_str);

	return 0;
}

int draft() {
	system("newv2.exe");

	return 0;
}