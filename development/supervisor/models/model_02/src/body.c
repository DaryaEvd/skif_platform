#include "../include/body.h"
#include "../include/variables.h"
#include <math.h>

extern double x_s, y_s, z_s, xSampleSize, ySampleSize, zSampleSize;

int inBody() {
	if ((fabs(x_s) <= xSampleSize / 2) && (fabs(y_s) <= ySampleSize / 2) && (fabs(z_s) <= zSampleSize / 2)) return 1;
	return 0;
}