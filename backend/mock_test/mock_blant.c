//DATA FORMAT: source + "\t" + interaction + "\t" + target + "\t" + confidence + "\n"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#define MAX_LINE_LEN 4096

int main(int argc, char *argv[]) {
    if (argc != 2) {
        fprintf(stderr, "Usage: %s <file.txt>\n", argv[0]);
        return EXIT_FAILURE;
    }

    const char *filename = argv[1];
    FILE *fp = fopen(filename, "r");
    if (!fp) {
        fprintf(stderr, "Error: could not open file '%s'\n", filename);
        return EXIT_FAILURE;
    }

    char line[MAX_LINE_LEN];
    while (fgets(line, sizeof(line), fp)) {
        fputs(line, stdout);
    }

    if (ferror(fp)) {
        fprintf(stderr, "Error: failed while reading '%s'\n", filename);
        fclose(fp);
        return EXIT_FAILURE;
    }

    fclose(fp);
    return EXIT_SUCCESS;
}