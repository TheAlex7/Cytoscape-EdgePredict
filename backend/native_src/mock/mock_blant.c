#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

#define MAX_LINE_LEN 4096
const char *MOCK_DATA_PATH = "native_src/mock/mock_output.txt";

int main(int argc, char *argv[]) {
    if (argc != 2) {
        fprintf(stderr, "Usage: %s <file.txt>\n", argv[0]);
        return EXIT_FAILURE;
    }

    // const char *filename = MOCK_DATA_PATH;
    const char *filename = argv[1]; // switch to this to get echo 
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

    // Loop every 2s for 1m 
    for (int i = 0; i < 30; i++) {
        fprintf(stderr, "Heartbeat %d\n", i+1);
        fflush(stderr);  
        sleep(2);
    }
    return EXIT_SUCCESS;
}