#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>

#define MAX_LINE_LEN 4096

int main(int argc, char *argv[]) {
    if (argc != 3) {
        fprintf(stderr, "Usage: %s <file_stderr> <file_stdout>\n", argv[0]);
        return EXIT_FAILURE;
    }

    
    FILE *fp1, *fp2;
    fp1 = fopen(argv[1], "r");
    if (!fp1) {
        fprintf(stderr, "Error: could not open file '%s'\n", argv[1]);
        return EXIT_FAILURE;
    }
    
    char line[MAX_LINE_LEN];
    while (fgets(line, sizeof(line), fp1) != NULL) {
        fprintf(stderr, "%s", line);
        fflush(stderr); 
        sleep(1);
    }
    fclose(fp1);

    fp2 = fopen(argv[2], "r");
    if (!fp2) {
        fprintf(stderr, "Error: could not open file '%s'\n", argv[2]);
        return EXIT_FAILURE;
    }

    while (fgets(line, sizeof(line), fp2) != NULL) {
        printf("%s", line);
        fflush(stdout); 
    }
    fclose(fp2);

    return 0;
}