#include <stdio.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>
#include "../cJSON/cJSON.h"
#include "../include/setting.h"
#include "../include/variables.h"
#include <math.h>

extern double c_x, c_y, c_z, s_x, s_y, s_z;
extern double omega, kappa, phi, d_x, d_y, d_z;
extern double xSampleSize, ySampleSize, zSampleSize;
extern double theta, beta, gammaValue, sU, sB, sR, sL;
extern double E_start, E_end;
extern int t;
extern double rho, M;
extern double* s_list;
extern double cos_a, cos_b, cos_c, sin_a, sin_b, sin_c;
extern double R_s[9], R_d[9];


char* readFile(const char* filename) {
	FILE* file = fopen(filename, "r");
	if (!file) {
		printf("File opening error. File: \"%s\".\n", filename);
		return NULL;
	}

	fseek(file, 0, SEEK_END);
	long len = ftell(file);
	fseek(file, 0, SEEK_SET);

	char* buffer = (char*)malloc(len + 1);
	if (!buffer) {
		fclose(file);
		printf("Memory error. File: %s.\n", filename);
		return NULL;
	}
	size_t read_len = fread(buffer, 1, len, file);
	buffer[read_len] = '\0';
	return buffer;
}

/*
int setData() {
	char* buffer = readFile("/input/start.json");
    if (!buffer) {
        fprintf(stderr, "start.json not ready\n");
        exit(1);
    }
	cJSON* json_data = cJSON_Parse(buffer);
	if (!json_data) {
        fprintf(stderr, "JSON parse error\n");
        free(buffer);
        exit(1);
    }
	cJSON* cur_item = NULL;
	int index = 0;
	cJSON_ArrayForEach(cur_item, json_data) {
		if (cJSON_IsNumber(cur_item)) {
			switch (index) {
				case 0: c_x = cur_item->valuedouble; break;
				case 1: c_y = cur_item->valuedouble; break;
				case 2: c_z = cur_item->valuedouble; break;
				case 3: s_x = cur_item->valuedouble; break;
				case 4: s_y = cur_item->valuedouble; break;
				case 5: s_z = cur_item->valuedouble; break;
				case 6: omega = cur_item->valuedouble; break;
				case 7: kappa = cur_item->valuedouble; break;
				case 8: phi = cur_item->valuedouble; break;
				case 9: xSampleSize = cur_item->valuedouble;  break;
				case 10: ySampleSize = cur_item->valuedouble; break;
				case 11: zSampleSize = cur_item->valuedouble; break;
				case 12: d_x = cur_item->valuedouble; break;
				case 13: d_y = cur_item->valuedouble; break;
				case 14: d_z = cur_item->valuedouble; break;
				case 15: theta = cur_item->valuedouble; break;
				case 16: beta = cur_item->valuedouble; break;
				case 17: gammaValue= cur_item->valuedouble; break;
				case 18: sU = cur_item->valuedouble; break;
				case 19: sB = cur_item->valuedouble; break;
				case 20: sR = cur_item->valuedouble; break;
				case 21: sL = cur_item->valuedouble; break;
				case 22: E_start = cur_item->valuedouble; break;
				case 23: E_end = cur_item->valuedouble; break;
				default: t = cur_item->valueint;
			}
			++index;
		}
		else
			printf("An error occured during data setting. The item is not a number: %s.\n", cur_item->valuestring);
	}
	cJSON_Delete(json_data);
	free(buffer);
	return 0;
}
*/

///*
int setData(void) {
    char* buffer = readFile("/input/start.json");
    if (!buffer) {
        fprintf(stderr, "start.json not ready\n");
        return -1;
    }

    cJSON* json = cJSON_Parse(buffer);
    if (!json) {
        fprintf(stderr, "JSON parse error\n");
        free(buffer);
        return -1;
    }

     #define GET_NUM(name, var)                                      \
            do {                                                        \
                cJSON* item = cJSON_GetObjectItemCaseSensitive(json, name); \
                if (!cJSON_IsNumber(item)) {                            \
                    fprintf(stderr, "Missing or invalid field: %s\n", name); \
                    goto error;                                         \
                }                                                       \
                var = item->valuedouble;                                \
            } while (0)

        #define GET_INT(name, var)                                      \
            do {                                                        \
                cJSON* item = cJSON_GetObjectItemCaseSensitive(json, name); \
                if (!cJSON_IsNumber(item)) {                            \
                    fprintf(stderr, "Missing or invalid field: %s\n", name); \
                    goto error;                                         \
                }                                                       \
                var = item->valueint;                                   \
            } while (0)


//    /*
    GET_NUM("c_x", c_x);
        GET_NUM("c_y", c_y);
        GET_NUM("c_z", c_z);

        GET_NUM("s_x", s_x);
        GET_NUM("s_y", s_y);
        GET_NUM("s_z", s_z);

        GET_NUM("omega", omega);
        GET_NUM("kappa", kappa);
        GET_NUM("phi", phi);

        GET_INT("xSampleSize", xSampleSize);
        GET_INT("ySampleSize", ySampleSize);
        GET_INT("zSampleSize", zSampleSize);

        GET_NUM("d_x", d_x);
        GET_NUM("d_y", d_y);
        GET_NUM("d_z", d_z);

        GET_NUM("theta", theta);
        GET_NUM("beta", beta);
        GET_NUM("gammaValue", gammaValue);

        GET_NUM("sU", sU);
        GET_NUM("sB", sB);
        GET_NUM("sR", sR);
        GET_NUM("sL", sL);

        GET_NUM("E_start", E_start);
        GET_NUM("E_end", E_end);

        GET_INT("t", t);

        cJSON_Delete(json);
        free(buffer);
        return 0;

    error:
        cJSON_Delete(json);
        free(buffer);
        return -1;
}
//*/

int setSampleMaterial() {
	char* buffer = readFile("/json/sampleMaterial.json");
	cJSON* json_data = cJSON_Parse(buffer);
	cJSON* rho_json = cJSON_GetObjectItemCaseSensitive(json_data, "rho");
	cJSON* M_json = cJSON_GetObjectItemCaseSensitive(json_data, "molar_mass");
	cJSON* cross_sections = cJSON_GetObjectItemCaseSensitive(json_data, "cross_sections");

	int array_size = cJSON_GetArraySize(cross_sections);

	rho = rho_json->valuedouble;
	M = M_json->valuedouble;
	s_list = (double*)malloc((array_size * 4 + 3) * sizeof(double));


	cJSON* cur_item = NULL;
	int index = 0;
	cJSON_ArrayForEach(cur_item, cross_sections) {
		for (int i = 0; i < 4; i++) {
			cJSON* elem = cJSON_GetArrayItem(cur_item, i);
			s_list[index++] = elem->valuedouble * (1 + 999 * (i == 0));
		}
	}
	cJSON_Delete(json_data);
	return 0;
}

int rotateMatrices() {
	cos_a = cos(omega); sin_a = sin(omega);
	cos_b = cos(kappa); sin_b = sin(kappa);
	cos_c = cos(phi); sin_c = sin(phi);

	R_s[0] = cos_a * cos_b * cos_c - sin_a * sin_c;
	R_s[1] = -sin_a * cos_b * cos_c - cos_a * sin_c;
	R_s[2] = -sin_b * cos_c;
	R_s[3] = cos_a * cos_b * sin_c + sin_a * cos_c;
	R_s[4] = -sin_a * cos_b * sin_c + cos_a * cos_c;
	R_s[5] = -sin_b * sin_c;
	R_s[6] = cos_a * sin_b;
	R_s[7] = -sin_a * sin_b;
	R_s[8] = cos_b;

	cos_a = cos(theta); sin_a = sin(theta);
	cos_b = cos(beta); sin_b = sin(beta);
	cos_c = cos(gammaValue); sin_c = sin(gammaValue);

	R_d[0] = cos_a * cos_b * cos_c - sin_a * sin_c;
	R_d[1] = -sin_a * cos_b * cos_c - cos_a * sin_c;
	R_d[2] = -sin_b * cos_c;
	R_d[3] = cos_a * cos_b * sin_c + sin_a * cos_c;
	R_d[4] = -sin_a * cos_b * sin_c + cos_a * cos_c;
	R_d[5] = -sin_b * sin_c;
	R_d[6] = cos_a * sin_b;
	R_d[7] = -sin_a * sin_b;
	R_d[8] = cos_b;

	return 0;
}